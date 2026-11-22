package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.block.StargateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block entity type registrations.
 *
 * <p><i>FR —</i> Enregistrement des types de block entity.
 */
public final class ModBlockEntities {
    /**
     * Block entity type register. / <i>FR —</i> Registre des types de block entity.
     */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StargateMod.MODID);

    /**
     * Block entity of {@link ModBlocks#STARGATE_CONTROL}. / <i>FR —</i> Block entity de {@link ModBlocks#STARGATE_CONTROL}.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StargateBlockEntity>> STARGATE =
            BLOCK_ENTITY_TYPES.register("stargate",
                    () -> new BlockEntityType<>(StargateBlockEntity::new, ModBlocks.STARGATE_CONTROL.get()));

    private ModBlockEntities() {
    }

    /**
     * Hooks the register to the mod event bus.
     *
     * <p><i>FR —</i> Branche le registre sur le bus d'événements du mod.
     *
     * @param modEventBus the mod event bus / le bus d'événements du mod
     */
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
