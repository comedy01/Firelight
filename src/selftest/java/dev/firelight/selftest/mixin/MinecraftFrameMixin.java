package dev.firelight.selftest.mixin;

import dev.firelight.selftest.FirelightSelfTest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void firelightSelftest$frame(boolean advanceGameTime, CallbackInfo ci) {
        FirelightSelfTest.frame();
    }
}
