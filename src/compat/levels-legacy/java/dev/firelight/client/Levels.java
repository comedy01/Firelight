package dev.firelight.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ChunkStatus;

public final class Levels {
    private Levels() {
    }

    public static int minSectionY(LevelHeightAccessor level) {
        return level.getMinSection();
    }

    public static int maxSectionY(LevelHeightAccessor level) {
        return level.getMaxSection() - 1;
    }

    public static ChunkAccess chunk(ClientLevel level, int x, int z) {
        return level.getChunk(x, z, ChunkStatus.FULL, false);
    }

    public static int chunkX(ChunkPos pos) {
        return pos.x;
    }

    public static int chunkZ(ChunkPos pos) {
        return pos.z;
    }

    public static boolean mayHaveFlames(LevelChunkSection section) {
        return section != null && !section.hasOnlyAir() && section.maybeHas(Flames::maybeFlame);
    }
}
