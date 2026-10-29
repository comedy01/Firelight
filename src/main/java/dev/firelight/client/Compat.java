package dev.firelight.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class Compat {
    private static boolean looked;
    private static MethodHandle shaderPackInUse;
    private static Boolean dynamicLights;

    private Compat() {
    }

    public static boolean shaderPackInUse() {
        if (!looked) {
            looked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = MethodHandles.publicLookup().unreflect(api.getMethod("isShaderPackInUse"))
                        .bindTo(irisApi).asType(MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                shaderPackInUse = null;
            }
        }
        if (shaderPackInUse == null) {
            return false;
        }
        try {
            return (boolean) shaderPackInUse.invokeExact();
        } catch (Throwable e) {
            shaderPackInUse = null;
            return false;
        }
    }

    public static boolean dynamicLights() {
        if (dynamicLights == null) {
            dynamicLights = present("dev.wick.client.WickClient")
                    || present("dev.lambdaurora.lambdynlights.LambDynLights")
                    || present("org.thinkingstudio.ryoamiclights.RyoamicLights");
        }
        return dynamicLights;
    }

    private static boolean present(String name) {
        try {
            Class.forName(name, false, Compat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
