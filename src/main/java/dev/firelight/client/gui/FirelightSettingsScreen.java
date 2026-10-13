package dev.firelight.client.gui;

import dev.firelight.client.FirelightClient;
import dev.firelight.client.Texts;
import dev.firelight.config.FirelightConfig;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class FirelightSettingsScreen extends FirelightOptionsScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public FirelightSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("firelight.options.title"));
    }

    @Override
    protected void addOptions() {
        FirelightConfig config = FirelightClient.config();

        addRow(
                toggle("firelight.options.enabled", config::enabled, config::setEnabled),
                tooltip(new StrengthSlider(config.strength(), config::setStrength),
                        Texts.translatable("firelight.options.strength.tooltip")));
        addRow(
                toggle("firelight.options.torches", config::torches, config::setTorches),
                toggle("firelight.options.lanterns", config::lanterns, config::setLanterns));
        addRow(
                toggle("firelight.options.candles", config::candles, config::setCandles),
                toggle("firelight.options.fires", config::fires, config::setFires));
        addRow(
                toggle("firelight.options.furnaces", config::furnaces, config::setFurnaces),
                toggle("firelight.options.entities", config::entities, config::setEntities));
        addRow(resetButton(config), null);
    }

    @Override
    public void removed() {
        super.removed();
        FirelightClient.saveConfig();
    }

    private AbstractWidget resetButton(FirelightConfig config) {
        return tooltip(button(Texts.translatable("firelight.options.reset"), WIDTH, button -> {
            config.resetToDefaults();
            ScreenOpener.open(minecraft, new FirelightSettingsScreen(lastScreen, options));
        }), Texts.translatable("firelight.options.reset.tooltip"));
    }

    private AbstractWidget toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return tooltip(button(toggleLabel(key, getter.getAsBoolean()), WIDTH, button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(toggleLabel(key, getter.getAsBoolean()));
        }), Texts.translatable(key + ".tooltip"));
    }

    private static Component percentText(int value) {
        return Texts.literal(value + "%");
    }

    private static Component valueLabel(String key, Component value) {
        return Texts.translatable("options.generic_value", Texts.translatable(key), value);
    }

    private static Component toggleLabel(String key, boolean on) {
        return valueLabel(key, Texts.translatable(on ? "options.on" : "options.off"));
    }

    private static final class StrengthSlider extends AbstractSliderButton {
        private static final int MIN = FirelightConfig.MIN_STRENGTH_PERCENT;
        private static final int MAX = FirelightConfig.MAX_STRENGTH_PERCENT;
        private static final int STEP = 5;

        private final Consumer<Float> onChange;

        StrengthSlider(float initial, Consumer<Float> onChange) {
            super(0, 0, WIDTH, HEIGHT, Texts.empty(), (Math.round(initial * 100) - MIN) / (double) (MAX - MIN));
            this.onChange = onChange;
            updateMessage();
        }

        private int percent() {
            int raw = MIN + (int) Math.round(value * (MAX - MIN));
            return Math.max(MIN, Math.min(MAX, Math.round(raw / (float) STEP) * STEP));
        }

        @Override
        protected void updateMessage() {
            setMessage(valueLabel("firelight.options.strength", percentText(percent())));
        }

        @Override
        protected void applyValue() {
            int percent = percent();
            value = (percent - MIN) / (double) (MAX - MIN);
            onChange.accept(percent / 100f);
        }
    }
}
