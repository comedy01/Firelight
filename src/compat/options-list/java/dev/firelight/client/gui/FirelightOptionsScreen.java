package dev.firelight.client.gui;

import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public abstract class FirelightOptionsScreen extends OptionsSubScreen {
    protected FirelightOptionsScreen(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    protected static Button button(Component label, int width, Button.OnPress onPress) {
        return Button.builder(label, onPress).width(width).build();
    }

    protected <T extends AbstractWidget> T tooltip(T widget, Component text) {
        widget.setTooltip(Tooltip.create(text));
        return widget;
    }

    protected void addHeader(Component text) {
        list.addHeader(text);
    }

    protected void addRow(AbstractWidget left, AbstractWidget right) {
        list.addSmall(left, right);
    }
}
