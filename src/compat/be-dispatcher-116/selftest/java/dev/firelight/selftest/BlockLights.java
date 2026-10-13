package dev.firelight.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

final class BlockLights {
    private BlockLights() {
    }

    static int sample(BlockEntity blockEntity) {
        return -1;
    }

    static int entity(Minecraft mc, Entity entity) {
        return mc.getEntityRenderDispatcher().getPackedLightCoords(entity, 0f);
    }
}
