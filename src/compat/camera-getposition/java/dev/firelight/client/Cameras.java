package dev.firelight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class Cameras {
    private Cameras() {
    }

    public static Vec3 position(Minecraft mc) {
        return mc.gameRenderer.getMainCamera().getPosition();
    }
}
