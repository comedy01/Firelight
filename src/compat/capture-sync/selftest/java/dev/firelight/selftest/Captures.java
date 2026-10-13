package dev.firelight.selftest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;

import java.util.function.Consumer;

final class Captures {
    private Captures() {
    }

    static void grab(RenderTarget target, int factor, Consumer<NativeImage> callback) {
        callback.accept(Screenshot.takeScreenshot(target));
    }

    static int rgb(NativeImage image, int x, int y) {
        return image.getPixel(x, y);
    }

    static int[] abgr(NativeImage image) {
        return image.getPixelsABGR();
    }
}
