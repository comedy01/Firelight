package dev.firelight.neoforge;

import dev.firelight.client.FirelightClient;
import dev.firelight.client.gui.FirelightSettingsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FirelightClient.MOD_ID, dist = Dist.CLIENT)
public final class FirelightNeoForge {
    public FirelightNeoForge(IEventBus modBus, ModContainer container) {
        FirelightClient.init(FMLPaths.CONFIGDIR.get());

        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new FirelightSettingsScreen(parent, Minecraft.getInstance().options));
    }
}
