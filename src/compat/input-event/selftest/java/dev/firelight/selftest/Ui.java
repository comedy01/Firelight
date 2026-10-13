package dev.firelight.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class Ui {
    private Ui() {
    }

    static void click(Screen screen, AbstractWidget widget) {
        click(screen, widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
    }

    static void click(Screen screen, double x, double y) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    static int x(AbstractWidget widget) {
        return widget.getX();
    }

    static int y(AbstractWidget widget) {
        return widget.getY();
    }

    static void fov(Minecraft mc, int fov) {
        mc.options.fov().set(fov);
    }

    static void unlock(Minecraft mc) {
        mc.options.framerateLimit().set(260);
        mc.options.enableVsync().set(false);
    }
}
