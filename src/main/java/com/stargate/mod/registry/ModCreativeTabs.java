package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative mode tabs of the mod.
 *
 * <p><i>FR —</i> Onglets du mode créatif du mod.
 */
public final class ModCreativeTabs {
    /**
     * Creative tab register. / <i>FR —</i> Registre des onglets créatifs.
     */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StargateMod.MODID);

    /**
     * Items that only serve as icons; kept out of {@link ModItems} so they never show in the tab.
     * <i>FR —</i> Objets servant seulement d'icône ; hors de {@link ModItems} pour ne jamais apparaître dans l'onglet.
     */
    public static final DeferredRegister.Items ICON_ITEMS = DeferredRegister.createItems(StargateMod.MODID);

    /**
     * Icon of the "Stargate" tab: the point of origin glyph ({@code textures/item/tab_icon.png}).
     * <i>FR —</i> Icône de l'onglet « Stargate » : le glyphe du point d'origine.
     */
    public static final DeferredItem<Item> TAB_ICON = ICON_ITEMS.registerSimpleItem("tab_icon");

    /**
     * The "Stargate" tab: every item registered in {@link ModItems}, in registration order, so new blocks show
     * up here without extra code. Placed after the vanilla tabs.
     *
     * <p><i>FR —</i> L'onglet « Stargate » : tous les objets enregistrés dans {@link ModItems}, dans l'ordre
     * d'enregistrement ; les nouveaux blocs y apparaissent donc sans code supplémentaire. Placé après les
     * onglets vanilla.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARGATE = CREATIVE_MODE_TABS.register("stargate",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stargate"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> TAB_ICON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }

    /**
     * Hooks the register to the mod event bus.
     *
     * <p><i>FR —</i> Branche le registre sur le bus d'événements du mod.
     *
     * @param modEventBus the mod event bus / le bus d'événements du mod
     */
    public static void register(IEventBus modEventBus) {
        ICON_ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
