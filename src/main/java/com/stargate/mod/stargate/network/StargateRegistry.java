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
 *
 * <p><i>FR —</i> Toutes les portes du monde, sauvegardées avec les données de l'Overworld
 * ({@code data/stargate_registry.dat}). Les adresses sont uniques sur toutes les dimensions.
 */
public final class StargateRegistry extends SavedData implements GateDirectory {
    /**
     * A registered gate.
     *
     * <p><i>FR —</i> Une porte enregistrée.
     *
     * @param id       stable gate id / identifiant stable de la porte
     * @param address  the address that dials this gate / l'adresse qui appelle cette porte
     * @param location where the gate stands / l'emplacement de la porte
     */
    public record GateRecord(UUID id, StargateAddress address, GlobalPos location) {
        /**
         * Save format of a record.
         *
         * <p><i>FR —</i> Format de sauvegarde d'un enregistrement.
         */
        public static final Codec<GateRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(GateRecord::id),
                ADDRESS_CODEC.fieldOf("address").forGetter(GateRecord::address),
                GlobalPos.CODEC.fieldOf("location").forGetter(GateRecord::location)
        ).apply(i, GateRecord::new));
    }

    /**
     * Saves an address as its list of symbol ids; rejects invalid addresses on load.
     *
     * <p><i>FR —</i> Sauvegarde une adresse sous forme de liste d'identifiants ; rejette les adresses
     * invalides au chargement.
     */
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

    /**
     * Save format of the whole registry.
     *
     * <p><i>FR —</i> Format de sauvegarde du registre entier.
     */
    public static final Codec<StargateRegistry> CODEC = GateRecord.CODEC.listOf()
            .fieldOf("gates").codec()
            .xmap(StargateRegistry::new, r -> List.copyOf(r.byId.values()));

    /**
     * Saved-data type, for {@code server.getDataStorage().computeIfAbsent(TYPE)}.
     *
     * <p><i>FR —</i> Type de données sauvegardées, pour {@code server.getDataStorage().computeIfAbsent(TYPE)}.
     */
    public static final SavedDataType<StargateRegistry> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(StargateMod.MODID, "stargate_registry"), StargateRegistry::new, CODEC);

    /**
     * Give up after this many collisions when drawing a fresh address (never expected in practice).
     * <i>FR —</i> Abandon après ce nombre de collisions en tirant une adresse (jamais attendu en pratique).
     */
    private static final int MAX_ADDRESS_ATTEMPTS = 1000;

    private final Map<UUID, GateRecord> byId = new LinkedHashMap<>();
    private final Map<StargateAddress, UUID> byAddress = new HashMap<>();
    private final Map<GlobalPos, UUID> byLocation = new HashMap<>();

    /**
     * Creates an empty registry.
     *
     * <p><i>FR —</i> Crée un registre vide.
     */
    public StargateRegistry() {
    }

    /** Rebuilds the indexes from saved records, dropping duplicates. / <i>FR —</i> Reconstruit les index depuis la sauvegarde, en écartant les doublons. */
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
     *
     * <p><i>FR —</i> Enregistre une porte à {@code location} avec une nouvelle adresse aléatoire. Si une
     * porte y est déjà enregistrée, la renvoie telle quelle.
     *
     * @param location where the gate stands / l'emplacement de la porte
     * @param random   source for the address / source d'aléa pour l'adresse
     * @return the new or existing record / l'enregistrement nouveau ou existant
     * @throws IllegalStateException if no free address could be found / si aucune adresse libre n'a été
     *                               trouvée
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

    /**
     * Registers a gate with a chosen address.
     *
     * <p><i>FR —</i> Enregistre une porte avec une adresse choisie.
     *
     * @param location where the gate stands / l'emplacement de la porte
     * @param address  the wanted address / l'adresse voulue
     * @return the record, or empty if the address or the location is taken / l'enregistrement, ou vide si
     *         l'adresse ou l'emplacement est déjà pris
     */
    public Optional<GateRecord> register(GlobalPos location, StargateAddress address) {
        if (byAddress.containsKey(address) || byLocation.containsKey(location)) {
            return Optional.empty();
        }
        return Optional.of(add(new GateRecord(UUID.randomUUID(), address, location)));
    }

    /**
     * Removes a gate. Does not touch its wormhole: use {@link StargateNetwork#removeGate} for that.
     *
     * <p><i>FR —</i> Retire une porte. Ne touche pas à son trou de ver : utilisez
     * {@link StargateNetwork#removeGate} pour cela.
     *
     * @param id the gate id / l'identifiant de la porte
     * @return the removed record, or empty if unknown / l'enregistrement retiré, ou vide s'il est inconnu
     */
    public Optional<GateRecord> unregister(UUID id) {
        GateRecord r = byId.remove(id);
        if (r != null) {
            byAddress.remove(r.address());
            byLocation.remove(r.location());
            setDirty();
        }
        return Optional.ofNullable(r);
    }

    /**
     * Looks a gate up by id.
     *
     * <p><i>FR —</i> Recherche une porte par identifiant.
     *
     * @param id the gate id / l'identifiant de la porte
     * @return the record, or empty / l'enregistrement, ou vide
     */
    public Optional<GateRecord> get(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Looks a gate up by address.
     *
     * <p><i>FR —</i> Recherche une porte par adresse.
     *
     * @param address the address / l'adresse
     * @return the record, or empty / l'enregistrement, ou vide
     */
    public Optional<GateRecord> findByAddress(StargateAddress address) {
        return Optional.ofNullable(byAddress.get(address)).map(byId::get);
    }

    /**
     * Looks a gate up by location.
     *
     * <p><i>FR —</i> Recherche une porte par emplacement.
     *
     * @param location dimension and block position / dimension et position du bloc
     * @return the record, or empty / l'enregistrement, ou vide
     */
    public Optional<GateRecord> findAt(GlobalPos location) {
        return Optional.ofNullable(byLocation.get(location)).map(byId::get);
    }

    /**
     * All gates, in registration order.
     *
     * <p><i>FR —</i> Toutes les portes, dans l'ordre d'enregistrement.
     *
     * @return an unmodifiable view / une vue non modifiable
     */
    public Collection<GateRecord> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    // GateDirectory

    /**
     * {@inheritDoc}
     *
     * <p><i>FR —</i> Trouve la porte qui répond à {@code address}.
     */
    @Override
    public Optional<UUID> resolve(StargateAddress address) {
        return Optional.ofNullable(byAddress.get(address));
    }

    /**
     * {@inheritDoc}
     *
     * <p><i>FR —</i> Indique si {@code gate} est enregistrée.
     */
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
