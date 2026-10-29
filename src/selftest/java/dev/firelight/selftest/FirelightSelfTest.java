package dev.firelight.selftest;

import com.mojang.blaze3d.platform.NativeImage;
import org.apache.logging.log4j.LogManager;
import dev.firelight.client.Backend;
import dev.firelight.client.Compat;
import dev.firelight.client.RawUniforms;
import dev.firelight.client.FirelightClient;
import dev.firelight.client.FlameTracker;
import dev.firelight.client.FlickerFrame;
import dev.firelight.client.ShaderPatches;
import dev.firelight.client.SodiumHook;
import dev.firelight.client.Torches;
import dev.firelight.client.gui.FirelightSettingsScreen;
import dev.firelight.config.FirelightConfig;
import dev.firelight.core.FlameKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public final class FirelightSelfTest {
    private static final String WORLD = "firelight-selftest";
    private static final int TIMEOUT = 2400;

    private static volatile int frames;
    public static volatile boolean hideHud;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> shots = new ArrayList<>();
    private final Set<String> existingShots = new HashSet<>();
    private final List<Double> samples = new CopyOnWriteArrayList<>();
    private final Set<Integer> entityLights = new HashSet<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int seenFrames;
    private int patience;
    private double flickerRange;
    private double flickerJitter;
    private boolean shaders;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = TutorialSteps.NONE;
                ServerCompat.hideChat(mc);
                rememberShots(mc);
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc, WORLD);
                Worlds.create(mc, WORLD, true, 0L);
            }
            return;
        }
        int drawn = frames;
        if (drawn == seenFrames) {
            return;
        }
        seenFrames = drawn;
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    private static final long FILM_STEP = 1_000_000_000L / 30;
    private static volatile Film film;
    private static volatile float[] pan;

    private static final class Film {
        final String name;
        final java.util.concurrent.BlockingQueue<Object[]> queue = new java.util.concurrent.ArrayBlockingQueue<>(6);
        final Thread writer;
        final long start = System.nanoTime();
        Process process;
        long next = start;
        int written;
        int dropped;
        int carry;

        Film(File out, String name) {
            this.name = name;
            writer = new Thread(() -> {
                try {
                    while (true) {
                        Object[] item = queue.take();
                        if (item.length == 0) {
                            break;
                        }
                        write((byte[]) item[0], (int) item[1], (int) item[2], (int) item[3], out);
                    }
                    if (process != null) {
                        process.getOutputStream().close();
                        process.waitFor();
                    }
                } catch (IOException | InterruptedException e) {
                    log("film " + name + " failed: " + e);
                }
            }, "Firelight film");
            writer.start();
        }

        void write(byte[] pixels, int width, int height, int copies, File out) throws IOException {
            if (process == null) {
                process = new ProcessBuilder(System.getProperty("firelight.ffmpeg"), "-y", "-loglevel", "error",
                        "-f", "rawvideo", "-pix_fmt", "rgba", "-s", width + "x" + height, "-r", "30", "-i", "-",
                        "-vf", "crop=trunc(iw/2)*2:trunc(ih/2)*2", "-c:v", "libx264", "-crf", "16", "-preset", "fast", "-pix_fmt", "yuv420p", out.getPath())
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            }
            for (int i = 0; i < copies; i++) {
                process.getOutputStream().write(pixels);
                written++;
            }
        }
    }

    public static void frame() {
        frames++;
        float[] look = pan;
        Minecraft mc = Minecraft.getInstance();
        if (look != null && mc.player != null) {
            double t = Math.min(1.0, (System.nanoTime() - panStart) / (look[4] * 1.0E9));
            double s = t * t * (3 - 2 * t);
            float yaw = (float) (look[0] + (look[1] - look[0]) * s);
            float pitch = (float) (look[2] + (look[3] - look[2]) * s);
            // turn() exists on every version (setYRot/setXRot don't on 1.16.5); it scales its input by 0.15
            mc.player.turn((yaw - mc.player.getViewYRot(1.0F)) / 0.15, (pitch - mc.player.getViewXRot(1.0F)) / 0.15);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
        }
        Film current = film;
        if (current == null) {
            return;
        }
        long now = System.nanoTime();
        int copies = 0;
        while (current.next <= now) {
            copies++;
            current.next += FILM_STEP;
        }
        if (copies == 0) {
            return;
        }
        int count = copies;
        Captures.grab(Screens.renderTarget(mc), 1, image -> {
            try {
                int[] pixels = Captures.abgr(image);
                java.nio.ByteBuffer bytes = java.nio.ByteBuffer.allocate(pixels.length * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                bytes.asIntBuffer().put(pixels);
                int total = count + current.carry;
                if (current.queue.offer(new Object[]{bytes.array(), image.getWidth(), image.getHeight(), total})) {
                    current.carry = 0;
                } else {
                    current.carry = total;
                    current.dropped += count;
                }
            } finally {
                image.close();
            }
        });
    }

    private static long panStart;

    private void pan(float yaw0, float yaw1, float pitch0, float pitch1, float length) {
        then(0, () -> {
            panStart = System.nanoTime();
            pan = new float[]{yaw0, yaw1, pitch0, pitch1, length};
        });
    }

    private void still() {
        then(0, () -> pan = null);
    }

    private static void startFilm(Minecraft mc, String name) {
        try {
            File dir = new File(mc.gameDirectory, "film");
            Files.createDirectories(dir.toPath());
            film = new Film(new File(dir, name + ".mp4"), name);
            log("filming " + name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void stopFilm() {
        Film current = film;
        film = null;
        if (current != null) {
            try {
                current.queue.put(new Object[0]);
                current.writer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log("filmed " + current.name + ": " + current.written + " frames, " + current.dropped + " dropped");
        }
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void when(BooleanSupplier ready, Runnable action) {
        steps.add(new Step(0, ready, action));
    }

    private void within(int ticks, BooleanSupplier ready, Runnable action) {
        then(1, () -> patience = ticks);
        when(() -> ready.getAsBoolean() || --patience < 0, action);
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        nameShots(mc);
        if (failure == null) {
            log("ALL CHECKS PASSED");
        } else {
            log("FAILED: " + failure);
            failure.printStackTrace();
        }
        FirelightConfig config = FirelightClient.config();
        config.resetToDefaults();
        FirelightClient.saveConfig();
        mc.stop();
    }

    private void plan(Minecraft mc) {
        FirelightConfig config = FirelightClient.config();
        int floor = ServerCompat.FLOOR;
        boolean sodium = sodiumLoaded();

        when(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null, () -> {
            shaders = Compat.shaderPackInUse();
            log("world loaded, sodium " + sodium + ", shader pack " + shaders);
        });
        then(40, () -> {
            config.resetToDefaults();
            hideHud = true;
            run(mc, "gamerule doDaylightCycle false");
            run(mc, "gamerule advance_time false");
            run(mc, "time set 18000");
            run(mc, "weather clear");
            run(mc, "fill -7 " + (floor - 1) + " -7 7 " + (floor + 5) + " 7 minecraft:stone hollow");
            run(mc, "tp @p 0.5 " + floor + " 5.5 180 40");
        });
        if (Boolean.getBoolean("firelight.film")) {
            filmScene(mc);
            return;
        }
        if (Boolean.getBoolean("firelight.perf")) {
            perfScene(mc);
            return;
        }
        then(20, () -> {
            if (sodium) {
                log("patches: sodium " + ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN)
                        + ", iris " + ShaderPatches.applied(ShaderPatches.IRIS_TERRAIN));
                check(ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN) || shaders && ShaderPatches.applied(ShaderPatches.IRIS_TERRAIN),
                        "Sodium's terrain shader was not patched");
                // With a pack active from startup Iris compiles its own terrain program, so Sodium's may never get its globals.
                check(ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN + "-raw") || ShaderPatches.applied(ShaderPatches.SODIUM_GLOBALS)
                                || shaders && ShaderPatches.applied(ShaderPatches.IRIS_TERRAIN),
                        "Sodium's terrain shader got no flame data (no globals patch, no raw uniforms)");
            } else if (Backend.RAW_UNIFORMS) {
                check(ShaderPatches.applied(ShaderPatches.VANILLA_TERRAIN + "-raw"), "the vanilla terrain shader did not get raw uniforms");
            } else {
                check(ShaderPatches.applied(ShaderPatches.VANILLA_GLOBALS), "the vanilla globals were not patched");
                check(ShaderPatches.applied(ShaderPatches.VANILLA_TERRAIN), "the vanilla terrain shader was not patched");
            }
            check(FlameTracker.snapshot().count() == 0, "flames found in an empty dark room");
            run(mc, "setblock 0 " + floor + " 0 minecraft:torch");
        });
        within(60, () -> FlameTracker.snapshot().count() == 1, () -> {
            FlameTracker.Snapshot snapshot = FlameTracker.snapshot();
            check(snapshot.count() == 1, "the torch was not found (" + snapshot.count() + " flames)");
            check(snapshot.kinds()[0] == FlameKind.TORCH, "the torch is not a torch flame: " + snapshot.kinds()[0]);
            check(snapshot.x()[0] == 0 && snapshot.y()[0] == floor && snapshot.z()[0] == 0, "the torch is in the wrong place");
        });
        then(40, () -> {
            check(FlickerFrame.count() == expectedFlames(shaders), "wrong number of flames sent to the shader: " + FlickerFrame.count());
            if (sodium && !shaders && !ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN + "-raw")) {
                check(SodiumHook.ran(), "the Sodium hook never ran");
            }
            if (!shaders && (Backend.RAW_UNIFORMS || ShaderPatches.applied(ShaderPatches.SODIUM_TERRAIN + "-raw"))) {
                check(RawUniforms.uploads() > 0, "the flame uniforms were never uploaded");
                log("raw uniform uploads so far: " + RawUniforms.uploads());
            }
            screenshot(mc, "torch-room");
        });

        then(1, () -> config.setStrength(2f));
        sample(mc, "flicker on");
        then(1, () -> {
            flickerRange = range();
            flickerJitter = jitter();
            log(String.format(Locale.ROOT, "floor brightness with flicker: %s(jitter %.2f)", describe(), jitter()));
            if (!shaders) {
                check(flickerRange > 1.5, String.format(Locale.ROOT, "the floor does not flicker (range %.2f)", flickerRange));
            }
            config.setEnabled(false);
        });
        sample(mc, "flicker off");
        then(1, () -> {
            double still = range();
            log(String.format(Locale.ROOT, "floor brightness without flicker: %s(jitter %.2f)", describe(), jitter()));
            check(shaders || still < flickerRange * 0.4, String.format(Locale.ROOT, "the floor still changes with Firelight off (range %.2f)", still));
            check(FlickerFrame.count() == 0, "flames were still sent with Firelight off");
            config.setEnabled(true);
            config.setStrength(1f);
        });

        if (wickLoaded()) {
            // Wick lights the floor from the held torch, so that torch must be a flame of its own at the player's eye.
            then(1, () -> {
                config.setStrength(2f);
                run(mc, "item replace entity @p weapon.mainhand with minecraft:torch");
            });
            then(40, () -> screenshot(mc, "wick-held-torch"));
            sample(mc, "holding a torch with Wick");
            then(1, () -> {
                FlameTracker.Snapshot flames = FlameTracker.snapshot();
                int ex = (int) Math.floor(mc.player.getX());
                int ey = (int) Math.floor(mc.player.getEyeY());
                int ez = (int) Math.floor(mc.player.getZ());
                boolean held = false;
                for (int i = 0; i < flames.count(); i++) {
                    held |= flames.x()[i] == ex && flames.y()[i] == ey && flames.z()[i] == ez
                            && flames.kinds()[i] == FlameKind.TORCH && flames.levels()[i] == 14;
                }
                log(String.format(Locale.ROOT, "floor brightness holding a torch with Wick: %s(jitter %.2f, %.2f without the held torch)", describe(), jitter(), flickerJitter));
                check(held, "the torch held with Wick is not a flame at the player's eye (" + flames.count() + " flames)");
                config.setStrength(1f);
                run(mc, "item replace entity @p weapon.mainhand with minecraft:air");
            });
            then(40, () -> {});
        }

        if (irisLoaded()) {
            then(1, () -> setShaders(!shaders));
            then(120, () -> {
                boolean now = Compat.shaderPackInUse();
                log("toggled shaders mid-game, shader pack now " + now + ", flames sent " + FlickerFrame.count());
                check(now != shaders, "toggling Iris shaders did not take effect");
                check(FlickerFrame.count() == expectedFlames(now), "wrong flames sent after toggling shaders: " + FlickerFrame.count());
                screenshot(mc, "shaders-toggled");
                config.setStrength(2f);
            });
            sample(mc, "after toggling shaders");
            then(1, () -> {
                log(String.format(Locale.ROOT, "floor brightness after toggling shaders: %s", describe()));
                if (!Compat.shaderPackInUse()) {
                    check(range() > 1.5, String.format(Locale.ROOT, "the floor does not flicker after turning shaders off (range %.2f)", range()));
                } else if (ShaderPatches.applied(ShaderPatches.IRIS_TERRAIN)) {
                    check(range() > 1.5, String.format(Locale.ROOT, "the floor does not flicker under the shader pack (range %.2f)", range()));
                }
                config.setStrength(1f);
                setShaders(shaders);
            });
            then(120, () -> {
                check(Compat.shaderPackInUse() == shaders, "toggling Iris shaders back did not take effect");
                check(FlickerFrame.count() == expectedFlames(shaders), "wrong flames sent after toggling shaders back: " + FlickerFrame.count());
            });
        }
        then(5, () -> run(mc, "summon minecraft:armor_stand 1.5 " + floor + " 1.5"));
        for (int i = 0; i < 30; i++) {
            then(2, () -> {
                Entity stand = armorStand(mc);
                if (stand != null) {
                    entityLights.add(BlockLights.entity(mc, stand) & 0xFF);
                }
            });
        }
        then(1, () -> {
            log("armor stand block light values: " + entityLights);
            check(entityLights.size() >= 3, "the armor stand does not flicker by the torch: " + entityLights);
            entityLights.clear();
            config.setEntities(false);
        });
        for (int i = 0; i < 10; i++) {
            then(2, () -> {
                Entity stand = armorStand(mc);
                if (stand != null) {
                    entityLights.add(BlockLights.entity(mc, stand) & 0xFF);
                }
            });
        }
        then(1, () -> {
            check(entityLights.size() == 1, "the armor stand still flickers with Mobs & Players off: " + entityLights);
            config.setEntities(true);
            entityLights.clear();
            run(mc, "setblock -1 " + ServerCompat.FLOOR + " 1 minecraft:chest");
        });
        for (int i = 0; i < 30; i++) {
            then(2, () -> {
                BlockEntity chest = mc.level.getBlockEntity(new BlockPos(-1, ServerCompat.FLOOR, 1));
                int light = chest == null ? -1 : BlockLights.sample(chest);
                if (light >= 0) {
                    entityLights.add(light & 0xFF);
                }
            });
        }
        then(1, () -> {
            log("chest block light values: " + entityLights);
            check(entityLights.isEmpty() || entityLights.size() >= 3, "the chest does not flicker by the torch: " + entityLights);
            config.setTorches(false);
        });
        within(20, () -> FlameTracker.snapshot().count() == 0, () -> {
            check(FlameTracker.snapshot().count() == 0, "the torch still flickers with Torches off");
            config.setTorches(true);
            run(mc, "setblock 0 " + floor + " 0 minecraft:air");
        });
        within(40, () -> FlameTracker.snapshot().count() == 0, () -> {
            check(FlameTracker.snapshot().count() == 0, "a broken torch is still flickering");
            if (Torches.CANDLES) {
                run(mc, "setblock -3 " + floor + " 0 minecraft:candle[lit=false]");
            }
            run(mc, "setblock 3 " + floor + " 0 minecraft:campfire[lit=true]");
            run(mc, "setblock 0 " + (floor + 3) + " -3 minecraft:lantern[hanging=true]");
        });
        within(40, () -> FlameTracker.snapshot().count() == 2, () -> {
            FlameTracker.Snapshot snapshot = FlameTracker.snapshot();
            check(snapshot.count() == 2, "expected the campfire and the lantern, found " + snapshot.count() + " flames");
            Set<FlameKind> kinds = Set.of(snapshot.kinds());
            check(kinds.contains(FlameKind.CAMPFIRE) && kinds.contains(FlameKind.LANTERN), "wrong flame kinds: " + kinds);
            if (Torches.CANDLES) {
                run(mc, "setblock -3 " + floor + " 0 minecraft:candle[lit=true,candles=3]");
            }
        });
        within(40, () -> FlameTracker.snapshot().count() == (Torches.CANDLES ? 3 : 2), () -> {
            check(!Torches.CANDLES || Set.of(FlameTracker.snapshot().kinds()).contains(FlameKind.CANDLE), "the lit candle was not found");
            screenshot(mc, "mixed-flames");
        });

        then(20, () -> Screens.open(mc, new FirelightSettingsScreen(null, mc.options)));
        then(20, () -> {
            check(Screens.current(mc) instanceof FirelightSettingsScreen, "the settings screen did not open");
            screenshot(mc, "settings");
            AbstractSliderButton slider = slider(Screens.current(mc), "Flicker Strength");
            check(slider.getMessage().getString().endsWith("100%"), "the strength slider does not start at 100%: " + slider.getMessage().getString());
            Ui.click(Screens.current(mc), Ui.x(slider) + slider.getWidth() - 2.0, Ui.y(slider) + slider.getHeight() / 2.0);
            check(Math.abs(config.strength() - 3f) < 1.0E-4f, "the strength slider did not reach 300%: " + config.strength());
            check(slider.getMessage().getString().endsWith("300%"), "the strength slider label is not 300%: " + slider.getMessage().getString());
            Ui.click(Screens.current(mc), Ui.x(slider) + 2.0, Ui.y(slider) + slider.getHeight() / 2.0);
            check(Math.abs(config.strength() - 0.1f) < 1.0E-4f, "the strength slider did not reach 10%: " + config.strength());
            Ui.click(Screens.current(mc), Ui.x(slider) + 4.0 + (slider.getWidth() - 8.0) * (140.0 / 290.0), Ui.y(slider) + slider.getHeight() / 2.0);
            float middle = config.strength();
            check(middle > 1.2f && middle < 1.8f && Math.abs(middle * 20 - Math.round(middle * 20)) < 1.0E-3f,
                    "the strength slider does not land on 5% steps near 150%: " + middle);
            log("strength slider: 300%, 10%, then " + Math.round(middle * 100) + "%");
        });
        then(5, () -> {
            Screens.open(mc, null);
            FirelightClient.saveConfig();
            check(read(FirelightClient.configPath()).contains("\"strength\": " + config.strength()), "the strength was not saved: " + read(FirelightClient.configPath()));
            config.resetToDefaults();
            Screens.open(mc, new FirelightSettingsScreen(null, mc.options));
        });
        then(5, () -> check(slider(Screens.current(mc), "Flicker Strength").getMessage().getString().endsWith("100%"),
                "the strength slider did not come back at 100% after a reset"));
        then(5, () -> {
            Screens.open(mc, null);
            hideHud = false;
        });
    }

    private void filmScene(Minecraft mc) {
        FirelightConfig config = FirelightClient.config();
        int f = ServerCompat.FLOOR;
        then(10, () -> {
            run(mc, "fill -16 " + (f - 1) + " -16 16 " + (f + 7) + " 16 minecraft:air");
            run(mc, "fill -9 " + (f - 1) + " -9 9 " + (f + 5) + " 9 minecraft:stone_bricks hollow");
            run(mc, "fill -8 " + (f - 1) + " -8 8 " + (f - 1) + " 8 minecraft:spruce_planks");
            run(mc, "fill -8 " + (f + 5) + " -8 8 " + (f + 5) + " 8 minecraft:dark_oak_planks");
            run(mc, "fill -8 " + f + " -8 8 " + f + " -8 minecraft:mossy_stone_bricks");
            run(mc, "setblock -4 " + (f + 2) + " -8 minecraft:wall_torch[facing=south]");
            run(mc, "setblock 4 " + (f + 2) + " -8 minecraft:wall_torch[facing=south]");
            run(mc, "setblock -8 " + (f + 2) + " 3 minecraft:wall_torch[facing=east]");
            run(mc, "setblock 8 " + (f + 2) + " 3 minecraft:wall_torch[facing=west]");
            run(mc, "setblock -1 " + f + " -7 minecraft:bookshelf");
            run(mc, "setblock -1 " + (f + 1) + " -7 minecraft:bookshelf");
            run(mc, "setblock 1 " + f + " -7 minecraft:crafting_table");
            run(mc, "setblock 0 " + f + " -4 minecraft:campfire[lit=true]");
            run(mc, "summon minecraft:villager 2.5 " + f + " -2.5 {NoAI:1b,Rotation:[150f,0f]}");
            run(mc, "setblock -7 " + f + " -6 minecraft:furnace[facing=east,lit=true]");
            run(mc, "setblock -7 " + (f + 1) + " -6 minecraft:furnace[facing=east,lit=true]");
            run(mc, "setblock -3 " + (f + 4) + " -5 minecraft:lantern[hanging=true]");
            run(mc, "setblock -5 " + f + " 1 minecraft:lantern");
            run(mc, "summon minecraft:cat -4.0 " + f + " 3.0 {NoAI:1b,Rotation:[120f,0f]}");
            run(mc, "setblock 6 " + f + " 0 minecraft:barrel[facing=up]");
            run(mc, "setblock 6 " + (f + 1) + " 0 minecraft:candle[lit=true,candles=4]");
            run(mc, "setblock 5 " + f + " 1 minecraft:candle_cake[lit=true]");
            run(mc, "setblock 7 " + f + " 1 minecraft:candle[lit=true,candles=3]");
            run(mc, "summon minecraft:armor_stand 6.5 " + f + " -5.5 {Rotation:[220f,0f]}");
        });
        then(60, () -> {
            run(mc, "kill @e[type=minecraft:item]");
            run(mc, "tp @p 0.5 " + f + " 7.5 180 12");
            Ui.fov(mc, 70);
            config.setEnabled(false);
        });
        pan(180, 180, 12, 12, 1);
        then(40, () -> startFilm(mc, "1-vanilla"));
        seconds(4, FirelightSelfTest::stopFilm);
        then(5, () -> config.setEnabled(true));
        then(20, () -> startFilm(mc, "2-firelight"));
        seconds(8, FirelightSelfTest::stopFilm);
        pan(150, 210, 14, 10, 9);
        then(0, () -> startFilm(mc, "3-pan"));
        seconds(9, FirelightSelfTest::stopFilm);
        shot(mc, "4-campfire", -1.5, f, -0.5, 205, 235, 18, 14, 7);
        shot(mc, "5-candles", 3.5, f, 3.5, 215, 235, 16, 12, 6);
        shot(mc, "6-lantern", -1.5, f, 4.5, 140, 120, 18, 16, 6);
        shot(mc, "7-furnace", -3.5, f, -2.5, 145, 125, 10, 8, 6);
        then(0, () -> run(mc, "tp @p 0.5 " + f + " 7.5 180 12"));
        pan(180, 180, 12, 12, 1);
        then(30, () -> config.setStrength(3f));
        then(0, () -> startFilm(mc, "8-strength"));
        seconds(6, FirelightSelfTest::stopFilm);
        then(0, () -> config.setStrength(1f));
        then(20, () -> startFilm(mc, "9-end"));
        seconds(5, FirelightSelfTest::stopFilm);
        still();
        then(5, () -> hideHud = false);
    }

    private void shot(Minecraft mc, String name, double x, int y, double z, float yaw0, float yaw1, float pitch0, float pitch1, float length) {
        then(0, () -> run(mc, String.format(Locale.ROOT, "tp @p %.2f %d %.2f %.1f %.1f", x, y, z, yaw0, pitch0)));
        pan(yaw0, yaw0, pitch0, pitch0, 1);
        then(30, () -> {
        });
        pan(yaw0, yaw1, pitch0, pitch1, length);
        then(0, () -> startFilm(mc, name));
        seconds(length, FirelightSelfTest::stopFilm);
    }

    private int perfFrames;
    private long perfStart;

    private void perfScene(Minecraft mc) {
        FirelightConfig config = FirelightClient.config();
        int f = ServerCompat.FLOOR;
        then(10, () -> {
            Ui.unlock(mc);
            run(mc, "fill -40 " + (f - 1) + " -40 40 " + (f + 8) + " 40 minecraft:stone_bricks hollow");
            run(mc, "fill -39 " + (f - 1) + " -39 39 " + (f - 1) + " 39 minecraft:spruce_planks");
            for (int x = -36; x <= 36; x += 6) {
                for (int z = -36; z <= 36; z += 6) {
                    run(mc, "setblock " + x + " " + f + " " + z + " minecraft:torch");
                }
            }
            run(mc, "tp @p 0.5 " + (f + 5) + " 30.5 180 25");
        });
        then(100, () -> log("flames tracked: " + FlameTracker.snapshot().count()));
        for (boolean on : new boolean[]{false, true, false, true}) {
            then(20, () -> config.setEnabled(on));
            then(60, () -> {
                perfFrames = frames;
                perfStart = System.nanoTime();
            });
            seconds(4, () -> {
                double fps = (frames - perfFrames) / ((System.nanoTime() - perfStart) / 1.0E9);
                log(String.format(Locale.ROOT, "fps with Firelight %s: %.1f (flames sent %d)", on ? "on" : "off", fps, FlickerFrame.count()));
            });
        }
        then(5, () -> hideHud = false);
    }

    private long until;

    private void seconds(double length, Runnable action) {
        then(0, () -> until = System.nanoTime() + (long) (length * 1.0E9));
        when(() -> System.nanoTime() >= until, action);
    }

    private void sample(Minecraft mc, String label) {
        then(1, () -> {
            samples.clear();
            log("sampling floor brightness, " + label);
        });
        for (int i = 0; i < 16; i++) {
            then(3, () -> {
                run(mc, "tp @p 0.5 " + ServerCompat.FLOOR + " 5.5 180 40");
                Captures.grab(Screens.renderTarget(mc), downscale(Screens.renderTarget(mc)), this::measure);
            });
        }
        within(40, () -> samples.size() >= 16, () -> check(samples.size() >= 12, "only " + samples.size() + " brightness samples arrived"));
    }

    private static int downscale(com.mojang.blaze3d.pipeline.RenderTarget target) {
        for (int factor : new int[]{4, 2}) {
            if (target.width % factor == 0 && target.height % factor == 0) {
                return factor;
            }
        }
        return 1;
    }

    private void measure(NativeImage image) {
        try {
            int x0 = image.getWidth() * 35 / 100;
            int x1 = image.getWidth() * 65 / 100;
            int y0 = image.getHeight() * 45 / 100;
            int y1 = image.getHeight() * 75 / 100;
            double sum = 0;
            int count = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int c = Captures.rgb(image, x, y);
                    sum += 0.299 * (c >> 16 & 0xFF) + 0.587 * (c >> 8 & 0xFF) + 0.114 * (c & 0xFF);
                    count++;
                }
            }
            samples.add(sum / count);
        } finally {
            image.close();
        }
    }

    // Mean change between consecutive frames: flicker keeps moving, a one-time step (Wick rebuilding sections) barely counts.
    private double jitter() {
        double sum = 0;
        for (int i = 1; i < samples.size(); i++) {
            sum += Math.abs(samples.get(i) - samples.get(i - 1));
        }
        return samples.size() < 2 ? 0 : sum / (samples.size() - 1);
    }

    private double range() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double value : samples) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return samples.isEmpty() ? 0 : max - min;
    }

    private String describe() {
        StringBuilder out = new StringBuilder();
        for (double value : samples) {
            out.append(String.format(Locale.ROOT, "%.1f ", value));
        }
        return out.append(String.format(Locale.ROOT, "(range %.2f)", range())).toString();
    }

    private static AbstractSliderButton slider(GuiEventListener node, String caption) {
        AbstractSliderButton found = findSlider(node, caption);
        check(found != null, "no slider " + caption);
        return found;
    }

    private static AbstractSliderButton findSlider(GuiEventListener node, String caption) {
        if (node instanceof AbstractSliderButton slider && slider.getMessage().getString().startsWith(caption)) {
            return slider;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractSliderButton found = findSlider(child, caption);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Entity armorStand(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand) {
                return entity;
            }
        }
        return null;
    }

    private static boolean wickLoaded() {
        try {
            Class.forName("dev.wick.client.WickClient", false, FirelightSelfTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    // Under a shader pack flames only reach the GPU once the pack's terrain shader has been patched.
    private static int expectedFlames(boolean shaders) {
        return shaders && !ShaderPatches.applied(ShaderPatches.IRIS_TERRAIN) ? 0 : 1;
    }

    private static boolean irisLoaded() {
        try {
            Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, FirelightSelfTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static void setShaders(boolean enabled) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object config = api.getMethod("getConfig").invoke(instance);
            Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig").getMethod("setShadersEnabledAndApply", boolean.class).invoke(config, enabled);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("could not toggle Iris shaders", e);
        }
    }

    private static boolean sodiumLoaded() {
        for (String name : new String[]{"net.caffeinemc.mods.sodium.client.gl.shader.GlProgram", "me.jellysquid.mods.sodium.client.gl.shader.GlProgram",
                "net.caffeinemc.mods.sodium.client.render.chunk.UniformBufferManager"}) {
            try {
                Class.forName(name, false, FirelightSelfTest.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                continue;
            }
        }
        return false;
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> ServerCompat.runCommand(server, command));
    }

    private void screenshot(Minecraft mc, String name) {
        log("screenshot: " + name);
        shots.add(name);
        ServerCompat.screenshot(mc, Screens.renderTarget(mc));
    }

    private void rememberShots(Minecraft mc) {
        File[] files = new File(mc.gameDirectory, "screenshots").listFiles();
        if (files != null) {
            for (File file : files) {
                existingShots.add(file.getName());
            }
        }
    }

    private void nameShots(Minecraft mc) {
        File dir = new File(mc.gameDirectory, "screenshots");
        File[] files = dir.listFiles((parent, name) -> name.endsWith(".png") && !existingShots.contains(name));
        if (files == null) {
            return;
        }
        List<File> fresh = new ArrayList<>(List.of(files));
        fresh.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
        for (int i = 0; i < fresh.size() && i < shots.size(); i++) {
            File target = new File(dir, String.format(Locale.ROOT, "fl-%02d-%s.png", i, shots.get(i)));
            if ((!target.exists() || target.delete()) && fresh.get(i).renameTo(target)) {
                log("saved " + target.getName());
            }
        }
    }

    private static void deleteWorld(Minecraft mc, String name) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(name);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void log(String message) {
        LogManager.getLogger("firelight-selftest").info("[firelight-selftest] " + message);
    }
}
