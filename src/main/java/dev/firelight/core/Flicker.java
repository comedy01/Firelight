package dev.firelight.core;

public final class Flicker {
    public static final int PERIOD = 600;
    public static final int MAX_SOURCES = 64;
    public static final int FLOATS = 4 + MAX_SOURCES * 4;
    public static final float MARK = 1234.5f;

    private static final int[][] RATES = {
            {5, 11, 23},
            {3, 7, 13},
            {4, 9, 17},
            {6, 13, 29},
            {4, 9, 19},
            {7, 15, 31},
            {1, 3, 7}
    };
    private static final float[] AMPLITUDES = {0.11f, 0.06f, 0.06f, 0.14f, 0.15f, 0.2f, 0.05f};
    private static final float[] DIPS = {1.4f, 0.8f, 1.0f, 1.6f, 1.2f, 1.0f, 0.6f};

    private Flicker() {
    }

    public static int hash(int x) {
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    public static int seed(int x, int y, int z) {
        return hash(x * 73856093 ^ y * 19349663 ^ z * 83492791) & 0x3FF;
    }

    static float lattice(int seed, int i) {
        return (hash(i * 0x9E3779B9 ^ seed) & 0xFFFF) / 65535f * 2f - 1f;
    }

    static float noise(int seed, float x, int period) {
        float cell = (float) Math.floor(x);
        float f = x - cell;
        int i = Math.floorMod((int) cell, period);
        int j = (i + 1) % period;
        float s = f * f * (3f - 2f * f);
        float a = lattice(seed, i);
        return a + (lattice(seed, j) - a) * s;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    public static float value(FlameKind kind, int seed, float time) {
        int k = kind.ordinal();
        int[] rates = RATES[k];
        float n = 0.5f * noise(seed, time * rates[0], rates[0] * PERIOD)
                + 0.3f * noise(seed + 101, time * rates[1], rates[1] * PERIOD)
                + 0.2f * noise(seed + 202, time * rates[2], rates[2] * PERIOD);
        float dip = smoothstep(0.35f, 0.9f, noise(seed + 303, time, PERIOD));
        return AMPLITUDES[k] * (n - DIPS[k] * dip);
    }

    public static float encode(int level, float value) {
        return level * 8 + 4 + Math.max(-3.99f, Math.min(3.99f, value));
    }

    public static float factor(float[] frame, double x, double y, double z, float blockLevel) {
        int count = Math.min((int) frame[2], MAX_SOURCES);
        if (count <= 0 || blockLevel < 0.5f) {
            return 1f;
        }
        float weights = 0f;
        float sum = 0f;
        float best = 0f;
        for (int i = 0; i < count; i++) {
            int base = 4 + i * 4;
            float encoded = frame[base + 3];
            float level = (float) Math.floor(encoded / 8f);
            float distance = (float) (Math.abs(x - frame[base]) + Math.abs(y - frame[base + 1]) + Math.abs(z - frame[base + 2]));
            float reach = level - distance;
            if (reach <= 0f) {
                continue;
            }
            float weight = reach * reach;
            weights += weight;
            sum += weight * (encoded - level * 8f - 4f);
            best = Math.max(best, reach);
        }
        if (weights == 0f) {
            return 1f;
        }
        float share = Math.min(1f, Math.max(0f, (best + 3.5f - blockLevel) * 0.5f));
        return Math.max(0f, 1f + sum / weights * Math.min(1f, best / blockLevel) * share);
    }
}
