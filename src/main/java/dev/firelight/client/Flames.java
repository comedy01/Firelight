package dev.firelight.client;

import dev.firelight.config.FirelightConfig;
import dev.firelight.core.FlameKind;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class Flames {
    private Flames() {
    }

    public static boolean maybeFlame(BlockState state) {
        return kind(state) != null;
    }

    public static FlameKind kind(BlockState state) {
        Block block = state.getBlock();
        if (Torches.isTorch(block)) {
            return block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH ? FlameKind.SOUL : FlameKind.TORCH;
        }
        if (Torches.isLantern(block)) {
            return block == Blocks.SOUL_LANTERN ? FlameKind.SOUL : FlameKind.LANTERN;
        }
        if (block == Blocks.JACK_O_LANTERN) {
            return FlameKind.LANTERN;
        }
        if (Torches.isCandle(block)) {
            return lit(state) ? FlameKind.CANDLE : null;
        }
        if (block instanceof CampfireBlock) {
            if (!lit(state)) {
                return null;
            }
            return block == Blocks.SOUL_CAMPFIRE ? FlameKind.SOUL : FlameKind.CAMPFIRE;
        }
        if (block instanceof BaseFireBlock) {
            return block == Blocks.SOUL_FIRE ? FlameKind.SOUL : FlameKind.FIRE;
        }
        if (block instanceof AbstractFurnaceBlock) {
            return lit(state) ? FlameKind.FURNACE : null;
        }
        return null;
    }

    public static final int TORCHES = 0;
    public static final int LANTERNS = 1;
    public static final int CANDLES = 2;
    public static final int FIRES = 3;
    public static final int FURNACES = 4;

    public static int group(BlockState state) {
        Block block = state.getBlock();
        if (Torches.isTorch(block)) {
            return TORCHES;
        }
        if (Torches.isLantern(block) || block == Blocks.JACK_O_LANTERN) {
            return LANTERNS;
        }
        if (Torches.isCandle(block)) {
            return CANDLES;
        }
        if (block instanceof AbstractFurnaceBlock) {
            return FURNACES;
        }
        return FIRES;
    }

    public static boolean enabled(FirelightConfig config, int group) {
        return switch (group) {
            case TORCHES -> config.torches();
            case LANTERNS -> config.lanterns();
            case CANDLES -> config.candles();
            case FURNACES -> config.furnaces();
            default -> config.fires();
        };
    }

    private static boolean lit(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }
}
