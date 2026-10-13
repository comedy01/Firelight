package dev.firelight.mixin;

import dev.firelight.client.FirelightClient;
import dev.firelight.client.FlameTracker;
import dev.firelight.client.RawUniforms;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftTickMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void firelight$frame(boolean advanceGameTime, CallbackInfo ci) {
        RawUniforms.newFrame();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void firelight$tick(CallbackInfo ci) {
        try {
            FlameTracker.tick((Minecraft) (Object) this);
        } catch (RuntimeException e) {
            FirelightClient.fail(e);
        }
    }
}
