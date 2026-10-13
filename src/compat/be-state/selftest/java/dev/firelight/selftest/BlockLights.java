package dev.firelight.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

final class BlockLights {
    private BlockLights() {
    }

    static int sample(BlockEntity blockEntity) {
        BlockEntityRenderState state = new BlockEntityRenderState();
        BlockEntityRenderState.extractBase(blockEntity, state, null);
        return state.lightCoords;
    }

    static int entity(Minecraft mc, Entity entity) {
        var renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        return renderer.getPackedLightCoords(entity, 0f);
    }
}
