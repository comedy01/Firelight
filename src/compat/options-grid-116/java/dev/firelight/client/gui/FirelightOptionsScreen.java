package dev.firelight.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class FirelightOptionsScreen extends Screen {
    private static final int ROW = 24;
    private static final int TOP = 32;
    private static final int BOTTOM = 36;

    protected final Screen lastScreen;
    protected final Options options;
    private final Map<AbstractWidget, Component> tooltips = new IdentityHashMap<>();
    private final List<Row> rowWidgets = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private int rows;
    private int scroll;

    protected FirelightOptionsScreen(Screen lastScreen, Options options, Component title) {
        super(title);
        this.lastScreen = lastScreen;
        this.options = options;
    }

    protected abstract void addOptions();

    protected static Button button(Component label, int width, Button.OnPress onPress) {
        return new Button(0, 0, width, 20, label, onPress);
    }

    protected <T extends AbstractWidget> T tooltip(T widget, Component text) {
        tooltips.put(widget, text);
        return widget;
    }

    @Override
    protected void init() {
        tooltips.clear();
        rowWidgets.clear();
        headers.clear();
        rows = 0;
        addOptions();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        layout();
        addButton(new Button(width / 2 - 100, height - 27, 200, 20, CommonComponents.GUI_DONE,
                button -> onClose()));
    }

    protected void addHeader(Component text) {
        headers.add(new Header(text, rows));
        rows++;
    }

    protected void addRow(AbstractWidget left, AbstractWidget right) {
        rowWidgets.add(new Row(left, rows, -155));
        addButton(left);
        if (right != null) {
            rowWidgets.add(new Row(right, rows, 5));
            addButton(right);
        }
        rows++;
    }

    private int maxScroll() {
        return Math.max(0, rows * ROW - (height - TOP - BOTTOM));
    }

    private int rowY(int row) {
        return TOP + row * ROW - scroll;
    }

    private boolean rowVisible(int row) {
        int y = rowY(row);
        return y >= TOP - 2 && y + 20 <= height - BOTTOM + 2;
    }

    private void layout() {
        for (Row row : rowWidgets) {
            row.widget.x = width / 2 + row.offset;
            row.widget.y = rowY(row.row);
            row.widget.visible = rowVisible(row.row);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(delta * ROW)));
        layout();
        return true;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, 15, 0xFFFFFF);
        for (Header header : headers) {
            if (rowVisible(header.row)) {
                drawCenteredString(poseStack, font, header.text, width / 2,
                        rowY(header.row) + (20 - font.lineHeight) / 2 + 1, 0xFFFFFF);
            }
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
        for (Map.Entry<AbstractWidget, Component> entry : tooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget.visible
                    && mouseX >= widget.x && mouseX < widget.x + widget.getWidth()
                    && mouseY >= widget.y && mouseY < widget.y + widget.getHeight()) {
                renderTooltip(poseStack, font.split(entry.getValue(), 200), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    private static final class Row {
        final AbstractWidget widget;
        final int row;
        final int offset;

        Row(AbstractWidget widget, int row, int offset) {
            this.widget = widget;
            this.row = row;
            this.offset = offset;
        }
    }

    private static final class Header {
        final Component text;
        final int row;

        Header(Component text, int row) {
            this.text = text;
            this.row = row;
        }
    }
}
