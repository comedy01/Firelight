package dev.firelight.mixin;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import dev.firelight.client.RawUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Iris binds its own programs, so the raw flame uniforms are uploaded whenever any program is used.
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
