package dev.firelight.client;

import java.lang.reflect.Method;

public final class Compat {
    private static boolean looked;
    private static Object irisApi;
    private static Method shaderPackInUse;

    private Compat() {
    }

    public static boolean shaderPackInUse() {
        if (!looked) {
            looked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                shaderPackInUse = api.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError e) {
                irisApi = null;
            }
        }
        if (irisApi == null) {
            return false;
        }
        try {
            return (Boolean) shaderPackInUse.invoke(irisApi);
        } catch (ReflectiveOperationException | RuntimeException e) {
            irisApi = null;
            return false;
        }
    }
}
