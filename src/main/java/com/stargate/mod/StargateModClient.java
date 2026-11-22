package com.stargate.mod;

import com.stargate.mod.client.EventHorizonRenderer;
import com.stargate.mod.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point; never loaded on a dedicated server, so client classes are safe to use here.
 * Registers the config screen and the event horizon renderer.
 *
 * <p><i>FR —</i> Point d'entrée côté client uniquement ; jamais chargé sur un serveur dédié, on peut donc y
 * utiliser les classes client sans risque. Enregistre l'écran de configuration et le rendu de l'horizon des
 * événements.
 */
// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = StargateMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = StargateMod.MODID, value = Dist.CLIENT)
public class StargateModClient {
    /**
     * Called by FML on the client.
     *
     * <p><i>FR —</i> Appelé par FML côté client.
     *
     * @param container this mod's container / le conteneur de ce mod
     */
    public StargateModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * Registers the event horizon renderer on the gate's block entity.
     *
     * <p><i>FR —</i> Enregistre le rendu de l'horizon des événements sur le block entity de la porte.
     */
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.STARGATE.get(), EventHorizonRenderer::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        StargateMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        StargateMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
