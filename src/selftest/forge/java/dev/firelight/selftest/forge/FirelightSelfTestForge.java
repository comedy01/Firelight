package dev.firelight.selftest.forge;

import dev.firelight.selftest.FirelightSelfTest;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("firelight_selftest")
public final class FirelightSelfTestForge {
    public FirelightSelfTestForge() {
        FirelightSelfTest test = new FirelightSelfTest();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                test.tick();
            }
        });
    }
}
