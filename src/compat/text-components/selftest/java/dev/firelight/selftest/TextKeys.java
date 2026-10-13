package dev.firelight.selftest;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

final class TextKeys {
    private static final Object[] NONE = new Object[0];

    private TextKeys() {
    }

    static String key(Component text) {
        return text.getContents() instanceof TranslatableContents contents ? contents.getKey() : null;
    }

    static Object[] args(Component text) {
        return text.getContents() instanceof TranslatableContents contents ? contents.getArgs() : NONE;
    }
}
