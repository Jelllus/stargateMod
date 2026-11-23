package com.stargate.mod;

import org.slf4j.Logger;

import com.stargate.mod.registry.ModBlockEntities;
import com.stargate.mod.registry.ModBlocks;
import com.stargate.mod.registry.ModEntities;
import com.stargate.mod.registry.ModCreativeTabs;
import com.stargate.mod.registry.ModItems;
import com.stargate.mod.stargate.StargateConfig;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

/**
 * Mod entry point: registers the deferred registers and the gate config. Gate logic is wired through
 * {@link com.stargate.mod.stargate.network.StargateNetwork} and
 * {@link com.stargate.mod.stargate.command.StargateCommand}, which subscribe to events on their own.
 *
 * <p><i>FR —</i> Point d'entrée du mod : enregistre les registres différés et la config des portes. La logique des
 * portes est branchée via {@link com.stargate.mod.stargate.network.StargateNetwork} et
 * {@link com.stargate.mod.stargate.command.StargateCommand}, qui s'abonnent seuls aux événements.
 */
// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(StargateMod.MODID)
public class StargateMod {
    /** The mod id, must match {@code neoforge.mods.toml}. / <i>FR —</i> L'identifiant du mod, doit correspondre à {@code neoforge.mods.toml}. */
    public static final String MODID = "stargate";
    /** Shared mod logger. / <i>FR —</i> Logger partagé du mod. */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Called by FML when the mod loads: registers the deferred registers and the gate config.
     *
     * <p><i>FR —</i> Appelé par FML au chargement du mod : enregistre les registres différés et la
     * config des portes.
     *
     * @param modEventBus  the mod event bus / le bus d'événements du mod
     * @param modContainer this mod's container / le conteneur de ce mod
     */
    public StargateMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.SYNCED, StargateConfig.SPEC);
    }
}
