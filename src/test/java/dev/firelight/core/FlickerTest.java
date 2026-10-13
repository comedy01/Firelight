package dev.firelight.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlickerTest {
    @Test
    void staysWithinItsAmplitude() {
        for (FlameKind kind : FlameKind.values()) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            for (int i = 0; i < 20000; i++) {
                float value = Flicker.value(kind, 517, i * 0.013f);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            assertTrue(max <= 0.25f && min >= -0.6f, kind + " out of range: " + min + ".." + max);
            assertTrue(max - min > 0.05f, kind + " barely moves: " + min + ".." + max);
        }
    }

    @Test
    void loopsWithoutAJump() {
        for (FlameKind kind : FlameKind.values()) {
            float end = Flicker.value(kind, 99, Flicker.PERIOD - 1.0E-4f);
            float start = Flicker.value(kind, 99, 0f);
            assertEquals(start, end, 0.01f, kind + " jumps when the time wraps");
        }
    }

    @Test
    void neighboursFlickerOnTheirOwn() {
        int a = Flicker.seed(10, 64, 10);
        int b = Flicker.seed(11, 64, 10);
        assertNotEquals(a, b);
        int same = 0;
        for (int i = 0; i < 200; i++) {
            float t = i * 0.05f;
            if (Math.abs(Flicker.value(FlameKind.TORCH, a, t) - Flicker.value(FlameKind.TORCH, b, t)) < 0.005f) {
                same++;
            }
        }
        assertTrue(same < 40, "two torches side by side flicker together " + same + " times out of 200");
    }

    @Test
    void lightFarFromFlamesStaysPut() {
        float[] frame = new float[Flicker.FLOATS];
        frame[0] = 12.3f;
        frame[1] = 1f;
        frame[2] = 1f;
        frame[7] = Flicker.encode(14, -0.2f);
        assertEquals(1f, Flicker.factor(frame, 20, 0, 0, 10f));
        assertEquals(1f, Flicker.factor(frame, 1, 0, 0, 0f));
        assertEquals(0.8f, Flicker.factor(frame, 1.5, 0, 0, 12.5f), 1.0E-4f);
        assertEquals(0.9f, Flicker.factor(frame, 1.5, 0, 0, 25f), 1.0E-4f);
    }

    @Test
    void encodingKeepsLevelAndValue() {
        for (int level = 1; level <= 15; level++) {
            for (float value = -3.5f; value <= 3.5f; value += 0.37f) {
                float encoded = Flicker.encode(level, value);
                float decodedLevel = (float) Math.floor(encoded / 8f);
                assertEquals(level, decodedLevel);
                assertEquals(value, encoded - decodedLevel * 8f - 4f, 1.0E-4f);
            }
        }
    }
}
