package dev.firelight.core;

public enum FlameKind {
    TORCH,
    SOUL,
    LANTERN,
    CANDLE,
    CAMPFIRE,
    FIRE,
    FURNACE;

    private static final FlameKind[] VALUES = values();

    public static FlameKind byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : TORCH;
    }
}
