package dev.firelight.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class ScreenOpener {
    private ScreenOpener() {
    }

    static void open(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }
}
