package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registrations, in creative tab order. Naquadah is a resource only: no tool, weapon or armour is made
 * from it.
 *
 * <p><i>FR —</i> Enregistrement des objets, dans l'ordre de l'onglet créatif. Le naquadah est une simple
 * ressource : aucun outil, arme ou armure n'en est fait.
 */
public final class ModItems {
    /**
     * Item register. / <i>FR —</i> Registre des objets.
     */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StargateMod.MODID);

    /**
     * Item form of {@link ModBlocks#STARGATE_CONTROL}. / <i>FR —</i> Forme objet de {@link ModBlocks#STARGATE_CONTROL}.
     */
    public static final DeferredItem<BlockItem> STARGATE_CONTROL =
            ITEMS.registerSimpleBlockItem("stargate_control", ModBlocks.STARGATE_CONTROL);

    /**
     * Item form of {@link ModBlocks#STARGATE_CHEVRON}. / <i>FR —</i> Forme objet de {@link ModBlocks#STARGATE_CHEVRON}.
     */
    public static final DeferredItem<BlockItem> STARGATE_CHEVRON =
            ITEMS.registerSimpleBlockItem("stargate_chevron", ModBlocks.STARGATE_CHEVRON);

    /**
     * Item form of {@link ModBlocks#NAQUADAH_BLOCK}. / <i>FR —</i> Forme objet de {@link ModBlocks#NAQUADAH_BLOCK}.
     */
    public static final DeferredItem<BlockItem> NAQUADAH_BLOCK =
            ITEMS.registerSimpleBlockItem("naquadah_block", ModBlocks.NAQUADAH_BLOCK);

    /**
     * Item form of {@link ModBlocks#STARGATE_DHD}. / <i>FR —</i> Forme objet de {@link ModBlocks#STARGATE_DHD}.
     */
    public static final DeferredItem<BlockItem> STARGATE_DHD =
            ITEMS.registerSimpleBlockItem("stargate_dhd", ModBlocks.STARGATE_DHD);

    /**
     * Item form of {@link ModBlocks#NAQUADAH_ORE}. / <i>FR —</i> Forme objet de {@link ModBlocks#NAQUADAH_ORE}.
     */
    public static final DeferredItem<BlockItem> NAQUADAH_ORE =
            ITEMS.registerSimpleBlockItem("naquadah_ore", ModBlocks.NAQUADAH_ORE);

    /**
     * Item form of {@link ModBlocks#RAW_NAQUADAH_BLOCK}. / <i>FR —</i> Forme objet de {@link ModBlocks#RAW_NAQUADAH_BLOCK}.
     */
    public static final DeferredItem<BlockItem> RAW_NAQUADAH_BLOCK =
            ITEMS.registerSimpleBlockItem("raw_naquadah_block", ModBlocks.RAW_NAQUADAH_BLOCK);

    /**
     * Raw naquadah, dropped by the ores; smelts into an ingot. / <i>FR —</i> Naquadah brut, donné par les
     * minerais ; se fond en lingot.
     */
    public static final DeferredItem<Item> RAW_NAQUADAH = ITEMS.registerSimpleItem("raw_naquadah");

    /**
     * Naquadah ingot. / <i>FR —</i> Lingot de naquadah.
     */
    public static final DeferredItem<Item> NAQUADAH_INGOT = ITEMS.registerSimpleItem("naquadah_ingot");

    /**
     * Naquadah nugget, a ninth of an ingot. / <i>FR —</i> Pépite de naquadah, un neuvième de lingot.
     */
    public static final DeferredItem<Item> NAQUADAH_NUGGET = ITEMS.registerSimpleItem("naquadah_nugget");

    /**
     * Item form of {@link ModBlocks#BLUE_CRYSTAL_CLUSTER}. / <i>FR —</i> Forme objet de {@link ModBlocks#BLUE_CRYSTAL_CLUSTER}.
     */
    public static final DeferredItem<BlockItem> BLUE_CRYSTAL_CLUSTER =
            ITEMS.registerSimpleBlockItem("blue_crystal_cluster", ModBlocks.BLUE_CRYSTAL_CLUSTER);

    /**
     * Blue crystal shard, from the clusters; used to craft the gate's control block.
     * <i>FR —</i> Éclat de cristal bleu, tiré des amas ; sert à fabriquer le bloc de commande de la porte.
     */
    public static final DeferredItem<Item> BLUE_CRYSTAL_SHARD = ITEMS.registerSimpleItem("blue_crystal_shard");

    /**
     * Chevron, a V of naquadah ingots; crafting part of the gate's chevron block.
     * <i>FR —</i> Chevron, un V de lingots de naquadah ; composant du bloc chevron de la porte.
     */
    public static final DeferredItem<Item> CHEVRON = ITEMS.registerSimpleItem("chevron");

    /**
     * Spawn egg of the {@link ModEntities#REPLICATOR}. / <i>FR —</i> Œuf d'apparition du {@link ModEntities#REPLICATOR}.
     */
    public static final DeferredItem<SpawnEggItem> REPLICATOR_SPAWN_EGG =
            ITEMS.registerItem("replicator_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.REPLICATOR.get()));

    private ModItems() {
    }

    /**
     * Hooks the register to the mod event bus.
     *
     * <p><i>FR —</i> Branche le registre sur le bus d'événements du mod.
     *
     * @param modEventBus the mod event bus / le bus d'événements du mod
     */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
