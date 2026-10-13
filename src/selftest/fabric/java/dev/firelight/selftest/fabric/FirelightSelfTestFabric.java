package dev.firelight.selftest.fabric;

import dev.firelight.selftest.FirelightSelfTest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class FirelightSelfTestFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FirelightSelfTest test = new FirelightSelfTest();
        ClientTickEvents.END_CLIENT_TICK.register(client -> test.tick());
    }
}
