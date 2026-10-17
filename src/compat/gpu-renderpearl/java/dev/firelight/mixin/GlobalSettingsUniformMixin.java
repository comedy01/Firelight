package dev.firelight.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import dev.firelight.client.FirelightClient;
import dev.firelight.client.FlickerFrame;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlobalSettingsUniform.class)
abstract class GlobalSettingsUniformMixin {
    @Shadow
    @Final
    private GpuBuffer buffer;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/device/GpuDevice;createBuffer(Ljava/util/function/Supplier;IJ)Lcom/mojang/renderpearl/api/buffers/GpuBuffer;"),
            index = 2)
    private long firelight$size(long size) {
        return firelight$offset() + FlickerFrame.BYTES;
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void firelight$update(CallbackInfo ci, @Local(argsOnly = true) Vec3 cameraPos) {
        try {
            FlickerFrame.update(cameraPos);
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(buffer.slice(firelight$offset(), FlickerFrame.BYTES), FlickerFrame.bytes());
        } catch (RuntimeException e) {
            FirelightClient.fail(e);
        }
    }

    private static int firelight$offset() {
        return (GlobalSettingsUniform.UBO_SIZE + 15) & ~15;
    }
}
