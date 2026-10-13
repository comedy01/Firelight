package dev.firelight.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.firelight.client.gui.FirelightSettingsScreen;
import net.minecraft.client.Minecraft;

public final class FirelightModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new FirelightSettingsScreen(parent, Minecraft.getInstance().options);
    }
}
