package dev.firelight.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
        list.addSmall(new Header(text), null);
    }

    private static final class Header extends AbstractWidget {
        Header(Component text) {
            super(0, 0, 310, 20, text);
            active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2,
                    getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }

    protected void addRow(AbstractWidget left, AbstractWidget right) {
        list.addSmall(left, right);
    }
}
