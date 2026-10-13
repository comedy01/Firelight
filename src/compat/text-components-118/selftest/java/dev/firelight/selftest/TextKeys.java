package dev.firelight.selftest;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

final class TextKeys {
    private static final Object[] NONE = new Object[0];

    private TextKeys() {
    }

    static String key(Component text) {
        return text instanceof TranslatableComponent translatable ? translatable.getKey() : null;
    }

    static Object[] args(Component text) {
        return text instanceof TranslatableComponent translatable ? translatable.getArgs() : NONE;
    }
}
