package dev.firelight.selftest.mixin;

import dev.firelight.selftest.FirelightSelfTest;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererHandMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void firelightSelftest$hideHand(CallbackInfo ci) {
        if (FirelightSelfTest.hideHud) {
            ci.cancel();
        }
    }
}
