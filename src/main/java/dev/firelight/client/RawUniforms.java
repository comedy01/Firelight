package dev.firelight.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class RawUniforms {
    private static final Int2ObjectOpenHashMap<int[]> PROGRAMS = new Int2ObjectOpenHashMap<>();
    private static volatile boolean changed;
    private static volatile boolean any;
    private static int frame;
    private static int updatedFrame = -1;
    private static FloatBuffer info;
    private static FloatBuffer sources;
    private static long uploads;

    private RawUniforms() {
    }

    public static long uploads() {
        return uploads;
    }

    public static void shadersChanged() {
        changed = true;
        any = true;
    }

    // Program ids are reused after a delete, so cached locations are dropped whenever a program is created, linked or deleted.
    public static void forget(int program) {
        if (any) {
            PROGRAMS.remove(program);
        }
    }

    public static void newFrame() {
        frame++;
    }

    public static void onCurrentProgram() {
        if (any) {
            onUse(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
        }
    }

    public static void onUse(int program) {
        if (!any || program == 0) {
            return;
        }
        try {
            if (changed) {
                PROGRAMS.clear();
                changed = false;
            }
            int[] entry = PROGRAMS.get(program);
            if (entry == null) {
                entry = new int[]{GL20.glGetUniformLocation(program, "FirelightInfo"),
                        GL20.glGetUniformLocation(program, "FirelightSources"), -1};
                if (entry[0] >= 0 && entry[1] >= 0 && !matches(program)) {
                    entry[0] = -1;
                }
                PROGRAMS.put(program, entry);
            }
            if (entry[0] < 0 || entry[1] < 0 || entry[2] == frame) {
                return;
            }
            if (updatedFrame != frame) {
                updatedFrame = frame;
                FlickerFrame.update(Cameras.position(Minecraft.getInstance()));
                FloatBuffer all = FlickerFrame.bytes().asFloatBuffer();
                info = all.duplicate().position(0).limit(4).slice();
                // The shader stops at FirelightInfo.z sources, so the unused tail of the array is never sent.
                sources = all.duplicate().position(4).limit(4 + FlickerFrame.count() * 4).slice();
            }
            GL20.glUniform4fv(entry[0], info);
            if (sources.hasRemaining()) {
                GL20.glUniform4fv(entry[1], sources);
            }
            entry[2] = frame;
            uploads++;
        } catch (RuntimeException e) {
            FirelightClient.fail(e);
        }
    }

    // A pack may declare its own uniforms with these names, so only the exact vec4 and vec4[64] declarations are fed.
    private static boolean matches(int program) {
        int count = GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
        int found = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < count && found < 2; i++) {
                String name = GL20.glGetActiveUniform(program, i, size, type);
                if (name.equals("FirelightInfo")) {
                    if (type.get(0) != GL20.GL_FLOAT_VEC4 || size.get(0) != 1) {
                        return false;
                    }
                    found++;
                } else if (name.equals("FirelightSources") || name.equals("FirelightSources[0]")) {
                    if (type.get(0) != GL20.GL_FLOAT_VEC4 || size.get(0) != 64) {
                        return false;
                    }
                    found++;
                }
            }
        }
        return found == 2;
    }
}
