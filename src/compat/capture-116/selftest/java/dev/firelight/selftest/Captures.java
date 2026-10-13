package dev.firelight.selftest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;

import java.util.function.Consumer;

final class Captures {
    private Captures() {
    }

    static void grab(RenderTarget target, int factor, Consumer<NativeImage> callback) {
        callback.accept(Screenshot.takeScreenshot(target.width, target.height, target));
    }

    static int rgb(NativeImage image, int x, int y) {
        return image.getPixelRGBA(x, y);
    }

    static int[] abgr(NativeImage image) {
        int width = image.getWidth();
        int[] pixels = new int[width * image.getHeight()];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = image.getPixelRGBA(i % width, i / width);
        }
        return pixels;
    }
}
