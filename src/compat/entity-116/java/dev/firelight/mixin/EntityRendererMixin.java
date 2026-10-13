package dev.firelight.mixin;

import dev.firelight.client.FirelightClient;
import dev.firelight.client.FlickerFrame;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityRendererMixin {
    @Inject(method = "getPackedLightCoords", at = @At("RETURN"), cancellable = true)
    private void firelight$light(Entity entity, float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (!FirelightClient.config().entities()) {
            return;
        }
        int packed = cir.getReturnValueI();
        int flickered = FlickerFrame.flicker(packed, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        if (flickered != packed) {
            cir.setReturnValue(flickered);
        }
    }
}
