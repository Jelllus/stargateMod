package com.stargate.mod.stargate;

import com.stargate.mod.stargate.connection.Timings;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Gameplay settings ({@code stargate-synced.toml}, overridable per world in {@code syncedconfig/}). All
 * durations in ticks.
 *
 * <p><i>FR —</i> Réglages de jeu ({@code stargate-synced.toml}, remplaçables par monde dans
 * {@code syncedconfig/}). Toutes les durées sont en ticks.
 */
public final class StargateConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("gate");
    }

    /**
     * Ticks between two chevrons during an automatic dial.
     *
     * <p><i>FR —</i> Ticks entre deux chevrons pendant une numérotation automatique.
     */
    public static final ModConfigSpec.IntValue CHEVRON_DELAY = BUILDER
            .comment("Ticks between two chevrons when a gate dials automatically")
            .defineInRange("chevronDelayTicks", Timings.DEFAULT.chevronDelayTicks(), 0, 20 * 60);

    /**
     * Ticks for the vortex to stabilise after locking.
     *
     * <p><i>FR —</i> Ticks de stabilisation du vortex après le verrouillage.
     */
    public static final ModConfigSpec.IntValue ENGAGE_TICKS = BUILDER
            .comment("Ticks for the vortex to stabilise after the address locks (entering during this time is lethal)")
            .defineInRange("engageTicks", Timings.DEFAULT.engageTicks(), 0, 20 * 60);

    /**
     * Maximum lifetime of an open wormhole.
     *
     * <p><i>FR —</i> Durée de vie maximale d'un trou de ver ouvert.
     */
    public static final ModConfigSpec.IntValue MAX_OPEN_TICKS = BUILDER
            .comment("Maximum lifetime of an open wormhole (default 38 minutes)")
            .defineInRange("maxOpenTicks", Timings.DEFAULT.maxOpenTicks(), 1, Integer.MAX_VALUE);

    /**
     * Ticks for the event horizon to collapse.
     *
     * <p><i>FR —</i> Ticks d'effondrement de l'horizon des événements.
     */
    public static final ModConfigSpec.IntValue CLOSE_TICKS = BUILDER
            .comment("Ticks for the event horizon to collapse")
            .defineInRange("closeTicks", Timings.DEFAULT.closeTicks(), 0, 20 * 60);

    /**
     * Ticks without a new symbol after which a manual dial is cancelled.
     *
     * <p><i>FR —</i> Ticks sans nouveau symbole après lesquels une numérotation manuelle est annulée.
     */
    public static final ModConfigSpec.IntValue DIAL_TIMEOUT_TICKS = BUILDER
            .comment("A manual dial is cancelled after this many ticks without a new symbol")
            .defineInRange("dialTimeoutTicks", Timings.DEFAULT.dialTimeoutTicks(), 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    /**
     * The spec registered with NeoForge.
     *
     * <p><i>FR —</i> La spécification enregistrée auprès de NeoForge.
     */
    public static final ModConfigSpec SPEC = BUILDER.build();

    private StargateConfig() {
    }

    /**
     * The configured durations. Only valid once the config is loaded (server started).
     *
     * <p><i>FR —</i> Les durées configurées. Valable seulement une fois la config chargée (serveur
     * démarré).
     *
     * @return the timings / les durées
     */
    public static Timings timings() {
        return new Timings(CHEVRON_DELAY.getAsInt(), ENGAGE_TICKS.getAsInt(), MAX_OPEN_TICKS.getAsInt(),
                CLOSE_TICKS.getAsInt(), DIAL_TIMEOUT_TICKS.getAsInt());
    }
}
