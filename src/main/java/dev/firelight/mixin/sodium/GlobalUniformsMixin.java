package dev.firelight.mixin.sodium;

import dev.firelight.client.SodiumHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager$GlobalUniforms", remap = false)
abstract class GlobalUniformsMixin {
    @Inject(method = "write", at = @At("HEAD"), remap = false, require = 0)
    private void firelight$write(ByteBuffer buffer, CallbackInfo ci) {
        SodiumHook.write(buffer);
    }
}
