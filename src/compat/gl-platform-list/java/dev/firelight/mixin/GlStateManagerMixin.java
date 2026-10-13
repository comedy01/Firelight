package dev.firelight.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.firelight.client.RawUniforms;
import dev.firelight.client.ShaderPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GlStateManager.class, remap = false)
abstract class GlStateManagerMixin {
    @ModifyVariable(method = "glShaderSource", at = @At("HEAD"), argsOnly = true, remap = false)
    private static List<String> firelight$source(List<String> source) {
        return List.of(ShaderPatches.patch("vanilla", String.join("", source)));
    }

    @Inject(method = "_glUseProgram", at = @At("TAIL"), remap = false)
    private static void firelight$use(int program, CallbackInfo ci) {
        RawUniforms.onUse(program);
    }
}
