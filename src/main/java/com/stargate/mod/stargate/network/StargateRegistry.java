package com.stargate.mod.stargate.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.address.StargateAddress;
import com.stargate.mod.stargate.connection.GateDirectory;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every gate in the world, saved with the overworld's data ({@code data/stargate_registry.dat}).
 * Addresses are unique across all dimensions.
 */
public final class StargateRegistry extends SavedData implements GateDirectory {
    /** A registered gate. */
    public record GateRecord(UUID id, StargateAddress address, GlobalPos location) {
        public static final Codec<GateRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(GateRecord::id),
                ADDRESS_CODEC.fieldOf("address").forGetter(GateRecord::address),
                GlobalPos.CODEC.fieldOf("location").forGetter(GateRecord::location)
        ).apply(i, GateRecord::new));
    }

    public static final Codec<StargateAddress> ADDRESS_CODEC = Codec.INT.listOf().comapFlatMap(
            ids -> {
                int[] arr = ids.stream().mapToInt(Integer::intValue).toArray();
                try {
                    return DataResult.success(StargateAddress.of(arr));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            address -> java.util.Arrays.stream(address.toIds()).boxed().toList()
    );

    public static final Codec<StargateRegistry> CODEC = GateRecord.CODEC.listOf()
            .fieldOf("gates").codec()
            .xmap(StargateRegistry::new, r -> List.copyOf(r.byId.values()));

    public static final SavedDataType<StargateRegistry> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(StargateMod.MODID, "stargate_registry"), StargateRegistry::new, CODEC);

    /** Give up after this many collisions when drawing a fresh address (never expected in practice). */
    private static final int MAX_ADDRESS_ATTEMPTS = 1000;

    private final Map<UUID, GateRecord> byId = new LinkedHashMap<>();
    private final Map<StargateAddress, UUID> byAddress = new HashMap<>();
    private final Map<GlobalPos, UUID> byLocation = new HashMap<>();

    public StargateRegistry() {
    }

    private StargateRegistry(List<GateRecord> records) {
        for (GateRecord r : records) {
            if (byAddress.containsKey(r.address()) || byLocation.containsKey(r.location())) {
                StargateMod.LOGGER.warn("Dropping duplicate stargate entry {} on load", r);
                continue;
            }
            index(r);
        }
    }

    /**
     * Registers a gate at {@code location} with a fresh random address. If a gate is already registered
     * there, returns it unchanged.
     */
    public GateRecord register(GlobalPos location, RandomSource random) {
        Optional<GateRecord> existing = findAt(location);
        if (existing.isPresent()) {
            return existing.get();
        }
        for (int attempt = 0; attempt < MAX_ADDRESS_ATTEMPTS; attempt++) {
            StargateAddress address = StargateAddress.random(new java.util.Random(random.nextLong()));
            if (!byAddress.containsKey(address)) {
                return add(new GateRecord(UUID.randomUUID(), address, location));
            }
        }
        throw new IllegalStateException("Could not find a free stargate address");
    }

    /** Registers a gate with a chosen address. Empty if the address or the location is taken. */
    public Optional<GateRecord> register(GlobalPos location, StargateAddress address) {
        if (byAddress.containsKey(address) || byLocation.containsKey(location)) {
            return Optional.empty();
        }
        return Optional.of(add(new GateRecord(UUID.randomUUID(), address, location)));
    }

    public Optional<GateRecord> unregister(UUID id) {
        GateRecord r = byId.remove(id);
        if (r != null) {
            byAddress.remove(r.address());
            byLocation.remove(r.location());
            setDirty();
        }
        return Optional.ofNullable(r);
    }

    public Optional<GateRecord> get(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<GateRecord> findByAddress(StargateAddress address) {
        return Optional.ofNullable(byAddress.get(address)).map(byId::get);
    }

    public Optional<GateRecord> findAt(GlobalPos location) {
        return Optional.ofNullable(byLocation.get(location)).map(byId::get);
    }

    public Collection<GateRecord> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    // GateDirectory

    @Override
    public Optional<UUID> resolve(StargateAddress address) {
        return Optional.ofNullable(byAddress.get(address));
    }

    @Override
    public boolean exists(UUID gate) {
        return byId.containsKey(gate);
    }

    private GateRecord add(GateRecord r) {
        index(r);
        setDirty();
        return r;
    }

    private void index(GateRecord r) {
        byId.put(r.id(), r);
        byAddress.put(r.address(), r.id());
        byLocation.put(r.location(), r.id());
    }
}
