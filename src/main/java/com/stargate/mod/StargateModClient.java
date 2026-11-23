package com.stargate.mod;

import com.stargate.mod.client.DhdRenderer;
import com.stargate.mod.client.StargateRenderer;
import com.stargate.mod.client.replicator.ReplicatorRenderer;
import com.stargate.mod.registry.ModBlockEntities;
import com.stargate.mod.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only entry point; never loaded on a dedicated server, so client classes are safe to use here.
 * Registers the config screen and the gate and DHD renderers.
 *
 * <p><i>FR —</i> Point d'entrée côté client uniquement ; jamais chargé sur un serveur dédié, on peut donc y
 * utiliser les classes client sans risque. Enregistre l'écran de configuration et les rendus de la porte et
 * du DHD.
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
     * Registers the gate renderer (symbol track, event horizon), the key renderer (DHD) and the replicator renderer.
     *
     * <p><i>FR —</i> Enregistre le rendu de la porte (piste des symboles, horizon des événements) celui des touches (DHD) et celui du réplicateur.
     */
    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.STARGATE.get(), StargateRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DHD.get(), DhdRenderer::new);
        event.registerEntityRenderer(ModEntities.REPLICATOR.get(), ReplicatorRenderer::new);
    }
}
