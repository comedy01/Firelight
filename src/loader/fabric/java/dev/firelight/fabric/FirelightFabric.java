package dev.firelight.fabric;

import dev.firelight.client.FirelightClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class FirelightFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FirelightClient.init(FabricLoader.getInstance().getConfigDir());
    }
}
