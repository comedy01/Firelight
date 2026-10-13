package dev.firelight.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

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
                sources = all.duplicate().position(4).slice();
            }
            GL20.glUniform4fv(entry[0], info);
            GL20.glUniform4fv(entry[1], sources);
            entry[2] = frame;
            uploads++;
        } catch (RuntimeException e) {
            FirelightClient.fail(e);
        }
    }
}
