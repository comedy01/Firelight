package dev.firelight.mixin.iris;

import dev.firelight.client.ShaderPatches;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

// Iris 1.6 and older, before the move to the net.irisshaders package.
@Pseudo
@Mixin(targets = "net.coderbot.iris.pipeline.transform.TransformPatcher", remap = false)
abstract class LegacyTransformPatcherMixin {
    @Inject(method = "patchSodium", at = @At("RETURN"), remap = false, require = 0)
    private static void firelight$sodium(CallbackInfoReturnable<Map<Object, String>> cir) {
        Map<Object, String> shaders = cir.getReturnValue();
        if (shaders == null) {
            return;
        }
        for (Map.Entry<Object, String> entry : shaders.entrySet()) {
            if ("VERTEX".equals(String.valueOf(entry.getKey()))) {
                entry.setValue(ShaderPatches.iris("iris-sodium", entry.getValue()));
            }
        }
    }
}
