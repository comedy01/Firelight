package dev.firelight.selftest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

final class Ui {
    private Ui() {
    }

    static void click(Screen screen, double x, double y) {
        screen.mouseClicked(x, y, 0);
        screen.mouseReleased(x, y, 0);
    }

    static int x(AbstractWidget widget) {
        return widget.x;
    }

    static int y(AbstractWidget widget) {
        return widget.y;
    }

    static void fov(Minecraft mc, int fov) {
        mc.options.fov().set(fov);
    }

    static void unlock(Minecraft mc) {
        mc.options.framerateLimit().set(260);
        mc.options.enableVsync().set(false);
    }
}
