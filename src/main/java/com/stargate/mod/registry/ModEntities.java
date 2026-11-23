package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import com.stargate.mod.replicator.ReplicatorEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity type registrations.
 *
 * <p><i>FR —</i> Enregistrement des types d'entités.
 */
public final class ModEntities {
    /** Entity type register. / <i>FR —</i> Registre des types d'entités. */
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(StargateMod.MODID);

    /** The replicator mob. / <i>FR —</i> Le monstre réplicateur. */
    public static final DeferredHolder<EntityType<?>, EntityType<ReplicatorEntity>> REPLICATOR =
            ENTITY_TYPES.registerEntityType("replicator", ReplicatorEntity::new, MobCategory.MONSTER,
                    (factory, category) -> EntityType.Builder.of(factory, category)
                            .sized(0.9F, 0.35F)
                            .eyeHeight(0.25F)
                            .clientTrackingRange(8)
                            .notInPeaceful());

    private ModEntities() {
    }

    /**
     * Hooks the register and the attribute event to the mod event bus.
     *
     * <p><i>FR —</i> Branche le registre et l'événement d'attributs sur le bus d'événements du mod.
     *
     * @param modEventBus the mod event bus / le bus d'événements du mod
     */
    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(REPLICATOR.get(), ReplicatorEntity.createAttributes().build());
    }
}
