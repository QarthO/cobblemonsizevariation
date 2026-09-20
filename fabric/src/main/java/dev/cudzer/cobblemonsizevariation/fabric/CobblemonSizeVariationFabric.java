package dev.cudzer.cobblemonsizevariation.fabric;

import dev.cudzer.cobblemonsizevariation.CobblemonSizeVariation;
import dev.cudzer.cobblemonsizevariation.Platform;
import dev.cudzer.cobblemonsizevariation.data.CustomSizeDataManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import java.nio.file.Path;

public final class CobblemonSizeVariationFabric implements ModInitializer, Platform {
    @Override
    public void onInitialize() {
        CobblemonSizeVariation.init(this);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new CustomSizeDataManager());
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
                CobblemonSizeVariation.registerCommands(dispatcher));
        CobblemonSizeVariation.LOGGER.info("Server-only sizing enabled; no addon items or custom client packets registered");
    }

    public boolean isModInstalled(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
