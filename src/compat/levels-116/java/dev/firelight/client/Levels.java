package dev.firelight.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class Levels {
    private Levels() {
    }

    public static int minSectionY(ClientLevel level) {
        return 0;
    }

    public static int maxSectionY(ClientLevel level) {
        return 15;
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
        return section != null && !section.isEmpty() && section.maybeHas(Flames::maybeFlame);
    }
}
