package dev.firelight.client;

import dev.firelight.core.Flicker;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class FlickerFrame {
    public static final int BYTES = Flicker.FLOATS * 4;
    private static final long START = System.nanoTime();

    private static final float[] FRAME = new float[Flicker.FLOATS];
    private static final ByteBuffer BYTES_BUFFER = ByteBuffer.allocateDirect(BYTES).order(ByteOrder.nativeOrder());
    private static final FloatBuffer FLOATS_BUFFER = BYTES_BUFFER.asFloatBuffer();
    private static final float[] ZEROS = new float[Flicker.FLOATS];
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static int sent;

    private FlickerFrame() {
    }

    public static float time() {
        double seconds = (System.nanoTime() - START) / 1.0E9;
        return (float) (seconds % Flicker.PERIOD);
    }

    public static void update(Vec3 camera) {
        float[] frame = FRAME;
        float time = time();
        float strength = FirelightClient.config().strength();
        frame[0] = time;
        frame[1] = strength;
        frame[3] = Flicker.MARK;
        FlameTracker.Snapshot snapshot = FlameTracker.snapshot();
        // Under a shader pack the GPU only gets flames once the pack's terrain program has been patched; entities
        // and block entities keep flickering either way (their light is set on the CPU).
        int count = FirelightClient.active() && camera != null ? snapshot.count() : 0;
        frame[2] = count;
        sent = Compat.shaderPackInUse() && !ShaderPatches.applied(ShaderPatches.IRIS_TERRAIN) ? 0 : count;
        if (camera != null) {
            cameraX = camera.x;
            cameraY = camera.y;
            cameraZ = camera.z;
        }
        for (int i = 0; i < count; i++) {
            int base = 4 + i * 4;
            frame[base] = (float) (snapshot.x()[i] + 0.5 - cameraX);
            frame[base + 1] = (float) (snapshot.y()[i] + 0.5 - cameraY);
            frame[base + 2] = (float) (snapshot.z()[i] + 0.5 - cameraZ);
            frame[base + 3] = Flicker.encode(snapshot.levels()[i],
                    Flicker.value(snapshot.kinds()[i], snapshot.seeds()[i], time) * strength);
        }
        FloatBuffer out = FLOATS_BUFFER;
        int used = 4 + sent * 4;
        out.clear();
        out.put(frame, 0, used);
        out.put(ZEROS, 0, Flicker.FLOATS - used);
        out.put(2, sent);
    }

    public static ByteBuffer bytes() {
        BYTES_BUFFER.clear();
        return BYTES_BUFFER;
    }

    public static void writeTo(ByteBuffer target, int offset) {
        ByteBuffer out = target.duplicate();
        out.clear().position(offset);
        out.put(BYTES_BUFFER.duplicate().clear());
    }

    public static int flicker(int packed, double x, double y, double z) {
        int block = packed & 0xFF;
        if (block == 0 || FRAME[2] == 0f) {
            return packed;
        }
        float factor = Flicker.factor(FRAME, x - cameraX, y - cameraY, z - cameraZ, block / 16f);
        int flickered = Math.max(0, Math.min(240, Math.round(block * factor)));
        return packed & ~0xFF | flickered;
    }

    public static int count() {
        return sent;
    }
}
