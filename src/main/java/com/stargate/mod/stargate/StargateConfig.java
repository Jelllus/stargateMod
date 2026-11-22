package com.stargate.mod.stargate;

import com.stargate.mod.stargate.connection.Timings;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Gameplay settings ({@code stargate-synced.toml}, overridable per world in {@code syncedconfig/}). All durations in ticks. */
public final class StargateConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("gate");
    }

    public static final ModConfigSpec.IntValue CHEVRON_DELAY = BUILDER
            .comment("Ticks between two chevrons when a gate dials automatically")
            .defineInRange("chevronDelayTicks", Timings.DEFAULT.chevronDelayTicks(), 0, 20 * 60);

    public static final ModConfigSpec.IntValue ENGAGE_TICKS = BUILDER
            .comment("Ticks for the vortex to stabilise after the address locks (entering during this time is lethal)")
            .defineInRange("engageTicks", Timings.DEFAULT.engageTicks(), 0, 20 * 60);

    public static final ModConfigSpec.IntValue MAX_OPEN_TICKS = BUILDER
            .comment("Maximum lifetime of an open wormhole (default 38 minutes)")
            .defineInRange("maxOpenTicks", Timings.DEFAULT.maxOpenTicks(), 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue CLOSE_TICKS = BUILDER
            .comment("Ticks for the event horizon to collapse")
            .defineInRange("closeTicks", Timings.DEFAULT.closeTicks(), 0, 20 * 60);

    public static final ModConfigSpec.IntValue DIAL_TIMEOUT_TICKS = BUILDER
            .comment("A manual dial is cancelled after this many ticks without a new symbol")
            .defineInRange("dialTimeoutTicks", Timings.DEFAULT.dialTimeoutTicks(), 1, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private StargateConfig() {
    }

    public static Timings timings() {
        return new Timings(CHEVRON_DELAY.getAsInt(), ENGAGE_TICKS.getAsInt(), MAX_OPEN_TICKS.getAsInt(),
                CLOSE_TICKS.getAsInt(), DIAL_TIMEOUT_TICKS.getAsInt());
    }
}
