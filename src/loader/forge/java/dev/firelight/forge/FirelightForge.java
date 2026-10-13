package dev.firelight.forge;

import dev.firelight.client.FirelightClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(FirelightClient.MOD_ID)
public final class FirelightForge {
    public FirelightForge() {
        ModLoadingContext context = ModLoadingContext.get();
        ForgeClient.clientOnly(context);
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        FirelightClient.init(FMLPaths.CONFIGDIR.get());
        ForgeClient.register(context);
    }
}
