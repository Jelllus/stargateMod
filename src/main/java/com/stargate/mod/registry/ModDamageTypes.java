package com.stargate.mod.registry;

import com.stargate.mod.StargateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Damage types of the mod. They are data-driven: each key has a matching
 * {@code data/stargate/damage_type/<name>.json}.
 *
 * <p><i>FR —</i> Types de dégâts du mod. Ils sont définis par les données : chaque clé a un fichier
 * {@code data/stargate/damage_type/<nom>.json} correspondant.
 */
public final class ModDamageTypes {
    /**
     * Vaporised by an unstable vortex (kawoosh). / <i>FR —</i> Désintégré par un vortex instable (kawoosh).
     */
    public static final ResourceKey<DamageType> KAWOOSH =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(StargateMod.MODID, "kawoosh"));

    private ModDamageTypes() {
    }
}
