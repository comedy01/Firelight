package dev.firelight.selftest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.player.ChatVisiblity;

final class ServerCompat {
    static final int FLOOR = 4;

    private ServerCompat() {
    }

    static void runCommand(IntegratedServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack(), legacy(command));
    }

    private static String legacy(String command) {
        if (command.startsWith("item replace ")) {
            return "replaceitem " + command.substring("item replace ".length()).replace(" with ", " ");
        }
        return command;
    }

    static void hideChat(Minecraft mc) {
        mc.options.chatVisibility = ChatVisiblity.HIDDEN;
    }

    static void screenshot(Minecraft mc, RenderTarget target) {
        Screenshot.grab(mc.gameDirectory, target.width, target.height, target, message -> {
        });
    }
}
