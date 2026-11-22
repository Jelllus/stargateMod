package com.stargate.mod;

import org.slf4j.Logger;

import com.stargate.mod.registry.ModBlockEntities;
import com.stargate.mod.registry.ModBlocks;
import com.stargate.mod.registry.ModCreativeTabs;
import com.stargate.mod.registry.ModItems;
import com.stargate.mod.stargate.StargateConfig;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Mod entry point: registers the deferred registers and the configs. Gate logic is wired through
 * {@link com.stargate.mod.stargate.network.StargateNetwork} and
 * {@link com.stargate.mod.stargate.command.StargateCommand}, which subscribe to events on their own. The
 * {@code EXAMPLE_*} content comes from the MDK template and is meant to be replaced.
 *
 * <p><i>FR —</i> Point d'entrée du mod : enregistre les registres différés et les configs. La logique des
 * portes est branchée via {@link com.stargate.mod.stargate.network.StargateNetwork} et
 * {@link com.stargate.mod.stargate.command.StargateCommand}, qui s'abonnent seuls aux événements. Le
 * contenu {@code EXAMPLE_*} vient du template MDK et est destiné à être remplacé.
 */
// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(StargateMod.MODID)
public class StargateMod {
    /** The mod id, must match {@code neoforge.mods.toml}. / <i>FR —</i> L'identifiant du mod, doit correspondre à {@code neoforge.mods.toml}. */
    public static final String MODID = "stargate";
    /** Shared mod logger. / <i>FR —</i> Logger partagé du mod. */
    public static final Logger LOGGER = LogUtils.getLogger();
    /** Block register, namespace {@value #MODID}. / <i>FR —</i> Registre des blocs, espace de noms {@value #MODID}. */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    /** Item register. / <i>FR —</i> Registre des objets. */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    /** Creative tab register. / <i>FR —</i> Registre des onglets créatifs. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Template example block {@code stargate:example_block}. / <i>FR —</i> Bloc d'exemple du template {@code stargate:example_block}. */
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", p -> p.mapColor(MapColor.STONE));
    /** Item form of {@link #EXAMPLE_BLOCK}. / <i>FR —</i> Forme objet de {@link #EXAMPLE_BLOCK}. */
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    /** Template example food item {@code stargate:example_item}. / <i>FR —</i> Nourriture d'exemple du template {@code stargate:example_item}. */
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", p -> p.food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    /** Template example creative tab, placed after Combat. / <i>FR —</i> Onglet créatif d'exemple du template, placé après Combat. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());// Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    /**
     * Called by FML when the mod loads: registers the deferred registers, listeners and configs.
     *
     * <p><i>FR —</i> Appelé par FML au chargement du mod : enregistre les registres différés, les écouteurs
     * et les configs.
     *
     * @param modEventBus  the mod event bus / le bus d'événements du mod
     * @param modContainer this mod's container / le conteneur de ce mod
     */
    public StargateMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.LOCAL, Config.SPEC);
        modContainer.registerConfig(ModConfig.Type.SYNCED, StargateConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    /**
     * Template example: logs when the server starts.
     *
     * <p><i>FR —</i> Exemple du template : journalise le démarrage du serveur.
     *
     * @param event the event / l'événement
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
