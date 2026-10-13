package dev.firelight.selftest.neoforge;

import dev.firelight.selftest.FirelightSelfTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "firelight_selftest", dist = Dist.CLIENT)
public final class FirelightSelfTestNeoForge {
    public FirelightSelfTestNeoForge() {
        FirelightSelfTest test = new FirelightSelfTest();
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> test.tick());
    }
}
