package dev.firelight.mixin;

import dev.firelight.client.FlickerFrame;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderState.class)
abstract class BlockEntityRenderStateMixin {
    @Inject(method = "extractBase", at = @At("TAIL"))
    private static void firelight$light(BlockEntity blockEntity, BlockEntityRenderState state,
                                        ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        if (blockEntity.getLevel() != null) {
            state.lightCoords = FlickerFrame.flicker(state.lightCoords,
                    state.blockPos.getX() + 0.5, state.blockPos.getY() + 0.5, state.blockPos.getZ() + 0.5);
        }
    }
}
