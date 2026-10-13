package dev.firelight.client;

import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;

public final class Torches {
    public static final boolean CANDLES = true;

    private Torches() {
    }

    public static boolean isTorch(Block block) {
        return block instanceof BaseTorchBlock && !(block instanceof RedstoneTorchBlock);
    }

    public static boolean isCandle(Block block) {
        return block instanceof AbstractCandleBlock;
    }

    public static boolean isLantern(Block block) {
        return block instanceof LanternBlock;
    }
}
