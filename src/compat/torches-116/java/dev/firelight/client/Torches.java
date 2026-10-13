package dev.firelight.client;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Lantern;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TorchBlock;

public final class Torches {
    public static final boolean CANDLES = false;

    private Torches() {
    }

    public static boolean isTorch(Block block) {
        return block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock);
    }

    public static boolean isCandle(Block block) {
        return false;
    }

    public static boolean isLantern(Block block) {
        return block instanceof Lantern;
    }
}
