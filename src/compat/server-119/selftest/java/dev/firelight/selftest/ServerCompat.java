package dev.firelight.selftest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.player.ChatVisiblity;

final class ServerCompat {
    static final int FLOOR = -60;

    private ServerCompat() {
    }

    static void runCommand(IntegratedServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    static void hideChat(Minecraft mc) {
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
    }

    static void screenshot(Minecraft mc, RenderTarget target) {
        Screenshot.grab(mc.gameDirectory, target, message -> {
        });
    }
}
