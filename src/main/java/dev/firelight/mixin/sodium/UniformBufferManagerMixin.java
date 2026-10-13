package dev.firelight.mixin.sodium;

import dev.firelight.client.SodiumHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager", remap = false)
abstract class UniformBufferManagerMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuDataStorageMapped;<init>(Ljava/lang/String;III)V"),
            index = 1, remap = false, require = 0)
    private int firelight$blockSize(int size) {
        return SodiumHook.blockSize(size);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicUniformStorage;<init>(Ljava/lang/String;II)V"),
            index = 1, remap = false, require = 0)
    private int firelight$uniformBlockSize(int size) {
        return SodiumHook.blockSize(size);
    }
}
