package dev.firelight.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.firelight.client.RawUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GlStateManager.class, remap = false)
abstract class ProgramUseMixin {
    @Inject(method = "_glUseProgram", at = @At("TAIL"), remap = false)
    private static void firelight$use(int program, CallbackInfo ci) {
        RawUniforms.onUse(program);
    }

    @Inject(method = "glCreateProgram", at = @At("RETURN"), remap = false)
    private static void firelight$create(CallbackInfoReturnable<Integer> cir) {
        RawUniforms.forget(cir.getReturnValueI());
    }

    @Inject(method = {"glLinkProgram", "glDeleteProgram"}, at = @At("TAIL"), remap = false)
    private static void firelight$forget(int program, CallbackInfo ci) {
        RawUniforms.forget(program);
    }
}
