package dev.firelight.client;

import dev.firelight.config.FirelightConfig;
import dev.firelight.core.FlameKind;
import dev.firelight.core.Flicker;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public final class FlameTracker {
    public static final int RANGE = 40;
    private static final int[] NONE = new int[0];
    private static final int CACHE_LIMIT = 2048;

    private static final Long2ObjectOpenHashMap<Entry> CACHE = new Long2ObjectOpenHashMap<>();
    private static ClientLevel cachedLevel;
    private static final Candidates FOUND = new Candidates();

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private record Entry(LevelChunkSection section, int[] flames) {
    }

    public record Snapshot(int count, int[] x, int[] y, int[] z, int[] levels, int[] seeds, FlameKind[] kinds) {
        static final Snapshot EMPTY = new Snapshot(0, NONE, NONE, NONE, NONE, NONE, new FlameKind[0]);
    }

    private FlameTracker() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void blockChanged(BlockPos pos, BlockState before, BlockState after) {
        if (before != after && (Flames.maybeFlame(before) || Flames.maybeFlame(after))) {
            CACHE.remove(SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4));
        }
    }

    public static void chunkChanged(ClientLevel level, int chunkX, int chunkZ) {
        if (level != cachedLevel) {
            return;
        }
        for (int sy = Levels.minSectionY(level); sy <= Levels.maxSectionY(level); sy++) {
            CACHE.remove(SectionPos.asLong(chunkX, sy, chunkZ));
        }
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        FirelightConfig config = FirelightClient.config();
        if (level == null || !FirelightClient.active()) {
            snapshot = Snapshot.EMPTY;
            if (level == null) {
                forget();
            }
            return;
        }
        if (level != cachedLevel) {
            forget();
            cachedLevel = level;
        }
        Vec3 camera = Cameras.position(mc);
        int cx = (int) Math.floor(camera.x);
        int cy = (int) Math.floor(camera.y);
        int cz = (int) Math.floor(camera.z);

        Candidates found = FOUND;
        found.reset();
        int minSy = Math.max(SectionPos.blockToSectionCoord(cy - RANGE), Levels.minSectionY(level));
        int maxSy = Math.min(SectionPos.blockToSectionCoord(cy + RANGE), Levels.maxSectionY(level));
        for (int sx = SectionPos.blockToSectionCoord(cx - RANGE); sx <= SectionPos.blockToSectionCoord(cx + RANGE); sx++) {
            for (int sz = SectionPos.blockToSectionCoord(cz - RANGE); sz <= SectionPos.blockToSectionCoord(cz + RANGE); sz++) {
                ChunkAccess chunk = Levels.chunk(level, sx, sz);
                if (chunk == null) {
                    continue;
                }
                for (int sy = minSy; sy <= maxSy; sy++) {
                    for (int packed : flames(level, chunk, sx, sy, sz)) {
                        if (!Flames.enabled(config, packed >>> 20 & 7)) {
                            continue;
                        }
                        int x = (sx << 4) + (packed & 15);
                        int y = (sy << 4) + (packed >> 8 & 15);
                        int z = (sz << 4) + (packed >> 4 & 15);
                        int dx = x - cx;
                        int dy = y - cy;
                        int dz = z - cz;
                        int distance = dx * dx + dy * dy + dz * dz;
                        if (distance <= RANGE * RANGE) {
                            found.add(x, y, z, packed, distance);
                        }
                    }
                }
            }
        }
        if (Compat.dynamicLights()) {
            held(level, config, cx, cy, cz, found);
        }
        snapshot = found.nearest(Flicker.MAX_SOURCES);
    }

    private static void held(ClientLevel level, FirelightConfig config, int cx, int cy, int cz, Candidates found) {
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            int flame = Math.max(heldFlame(living.getMainHandItem()), heldFlame(living.getOffhandItem()));
            if (flame == 0 || !Flames.enabled(config, flame >>> 20 & 7)) {
                continue;
            }
            int x = (int) Math.floor(living.getX());
            int y = (int) Math.floor(living.getEyeY());
            int z = (int) Math.floor(living.getZ());
            int dx = x - cx;
            int dy = y - cy;
            int dz = z - cz;
            int distance = dx * dx + dy * dy + dz * dz;
            if (distance <= RANGE * RANGE) {
                found.add(x, y, z, flame, distance, Flicker.seed(entity.getId(), 0x5EED, 0));
            }
        }
    }

    private static int heldFlame(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return 0;
        }
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        FlameKind kind = Flames.kind(state);
        int light = Math.min(15, state.getLightEmission());
        if (kind == null || light <= 0) {
            return 0;
        }
        return kind.ordinal() << 12 | light << 16 | Flames.group(state) << 20;
    }

    private static final class Candidates {
        private int size;
        private int[] x = new int[64];
        private int[] y = new int[64];
        private int[] z = new int[64];
        private int[] packed = new int[64];
        private int[] seed = new int[64];
        private long[] order = new long[64];
        private long[] sorted = new long[64];

        void reset() {
            size = 0;
        }

        void add(int px, int py, int pz, int flame, int distance) {
            add(px, py, pz, flame, distance, Flicker.seed(px, py, pz));
        }

        void add(int px, int py, int pz, int flame, int distance, int flameSeed) {
            if (size == x.length) {
                int grown = size * 2;
                x = Arrays.copyOf(x, grown);
                y = Arrays.copyOf(y, grown);
                z = Arrays.copyOf(z, grown);
                packed = Arrays.copyOf(packed, grown);
                seed = Arrays.copyOf(seed, grown);
                order = Arrays.copyOf(order, grown);
            }
            x[size] = px;
            y[size] = py;
            z[size] = pz;
            packed[size] = flame;
            seed[size] = flameSeed;
            order[size] = (long) distance << 32 | size;
            size++;
        }

        Snapshot nearest(int limit) {
            if (size == 0) {
                return Snapshot.EMPTY;
            }
            if (sorted.length < size) {
                sorted = new long[order.length];
            }
            System.arraycopy(order, 0, sorted, 0, size);
            Arrays.sort(sorted, 0, size);
            int count = Math.min(limit, size);
            int[] xs = new int[count];
            int[] ys = new int[count];
            int[] zs = new int[count];
            int[] levels = new int[count];
            int[] seeds = new int[count];
            FlameKind[] kinds = new FlameKind[count];
            for (int i = 0; i < count; i++) {
                int c = (int) sorted[i];
                xs[i] = x[c];
                ys[i] = y[c];
                zs[i] = z[c];
                FlameKind kind = FlameKind.byIndex(packed[c] >>> 12 & 15);
                kinds[i] = kind;
                levels[i] = packed[c] >>> 16 & 15;
                seeds[i] = seed[c];
            }
            return new Snapshot(count, xs, ys, zs, levels, seeds, kinds);
        }
    }

    private static int[] flames(ClientLevel level, ChunkAccess chunk, int sx, int sy, int sz) {
        int sectionIndex = sy - Levels.minSectionY(level);
        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            return NONE;
        }
        LevelChunkSection section = sections[sectionIndex];
        long key = SectionPos.asLong(sx, sy, sz);
        Entry entry = CACHE.get(key);
        if (entry != null && entry.section() == section) {
            return entry.flames();
        }
        int[] flames = scan(section);
        if (CACHE.size() >= CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(key, new Entry(section, flames));
        return flames;
    }

    static int[] scan(LevelChunkSection section) {
        if (!Levels.mayHaveFlames(section)) {
            return NONE;
        }
        int[] found = new int[8];
        int count = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = section.getBlockState(x, y, z);
                    FlameKind kind = Flames.kind(state);
                    if (kind == null) {
                        continue;
                    }
                    int light = Math.min(15, state.getLightEmission());
                    if (light <= 0) {
                        continue;
                    }
                    if (count == found.length) {
                        found = Arrays.copyOf(found, count * 2);
                    }
                    found[count++] = x | z << 4 | y << 8 | kind.ordinal() << 12 | light << 16 | Flames.group(state) << 20;
                }
            }
        }
        return Arrays.copyOf(found, count);
    }

    public static void forget() {
        CACHE.clear();
        cachedLevel = null;
        snapshot = Snapshot.EMPTY;
    }
}
