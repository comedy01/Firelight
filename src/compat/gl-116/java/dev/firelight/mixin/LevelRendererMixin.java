package dev.firelight.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import dev.firelight.client.TerrainProgram;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    private static final String CHUNK_LAYER = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDD)V";

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = CHUNK_LAYER), require = 0)
    private void firelight$bind(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline,
                                Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        TerrainProgram.bind(poseStack.last().pose());
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = CHUNK_LAYER, shift = At.Shift.AFTER), require = 0)
    private void firelight$unbind(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline,
                                  Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projection, CallbackInfo ci) {
        TerrainProgram.unbind();
    }
}
