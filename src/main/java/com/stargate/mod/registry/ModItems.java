package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registrations. The DHD block item will be declared here too.
 *
 * <p><i>FR —</i> Enregistrement des objets. L'objet-bloc du DHD sera aussi déclaré ici.
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
