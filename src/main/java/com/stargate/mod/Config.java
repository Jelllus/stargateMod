package com.stargate.mod;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Example local config inherited from the NeoForge MDK template ({@code stargate-local.toml}). Gameplay
 * settings for gates live in {@link com.stargate.mod.stargate.StargateConfig}; this class can be removed
 * along with the example content of {@link StargateMod}.
 *
 * <p><i>FR —</i> Config locale d'exemple héritée du template MDK de NeoForge ({@code stargate-local.toml}).
 * Les réglages de jeu des portes sont dans {@link com.stargate.mod.stargate.StargateConfig} ; cette classe
 * pourra être supprimée avec le contenu d'exemple de {@link StargateMod}.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** Template example: log the dirt block at setup. / <i>FR —</i> Exemple du template : journaliser le bloc de terre au démarrage. */
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    /** Template example: a number logged at setup. / <i>FR —</i> Exemple du template : un nombre journalisé au démarrage. */
    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    /** Template example: text logged before the number. / <i>FR —</i> Exemple du template : texte journalisé avant le nombre. */
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    /** Template example: item ids logged at setup. / <i>FR —</i> Exemple du template : identifiants d'objets journalisés au démarrage. */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
