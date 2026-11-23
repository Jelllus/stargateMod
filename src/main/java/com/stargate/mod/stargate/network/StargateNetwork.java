package com.stargate.mod.stargate.network;

import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.StargateConfig;
import com.stargate.mod.stargate.address.StargateAddress;
import com.stargate.mod.stargate.connection.ConnectionManager;
import com.stargate.mod.stargate.connection.DialResult;
import com.stargate.mod.stargate.connection.GateDirectory;
import com.stargate.mod.stargate.connection.GateListener;
import com.stargate.mod.stargate.connection.StargateConnection;
import com.stargate.mod.stargate.connection.WormholeState;
import com.stargate.mod.stargate.network.StargateRegistry.GateRecord;
import com.stargate.mod.stargate.transport.TransportHandler;
import com.stargate.mod.stargate.address.StargateSymbol;
import com.stargate.mod.stargate.block.StargateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side entry point for gates: owns the {@link ConnectionManager} for the running server, ticks it,
 * and keeps it in step with the {@link StargateRegistry}.
 *
 * <p>After each tick, gates that changed are pushed to their {@link StargateBlockEntity} (if loaded),
 * which syncs them to clients.
 *
 * <p>Wormholes are not saved: they all close when the server stops.
 *
 * <p><i>FR —</i> Point d'entrée côté serveur pour les portes : possède le {@link ConnectionManager} du
 * serveur en cours, le fait avancer à chaque tick, et le garde en phase avec le {@link StargateRegistry}.
 *
 * <p>Après chaque tick, les portes modifiées sont transmises à leur {@link StargateBlockEntity} (s'il est
 * chargé), qui les synchronise avec les clients.
 *
 * <p>Les trous de ver ne sont pas sauvegardés : ils se ferment tous à l'arrêt du serveur.
 */
@EventBusSubscriber(modid = StargateMod.MODID)
public final class StargateNetwork {
    private static MinecraftServer server;
    private static ConnectionManager connections;

    private StargateNetwork() {
    }

    /**
     * The gate registry of a server, created on first access.
     *
     * <p><i>FR —</i> Le registre des portes d'un serveur, créé au premier accès.
     *
     * @param server the running server / le serveur en cours
     * @return the registry / le registre
     */
    public static StargateRegistry registry(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(StargateRegistry.TYPE);
    }

    /**
     * The connection manager of the running server.
     *
     * <p><i>FR —</i> Le gestionnaire de connexions du serveur en cours.
     *
     * @return the manager / le gestionnaire
     * @throws IllegalStateException if no server is running / si aucun serveur ne tourne
     */
    public static ConnectionManager connections() {
        if (connections == null) {
            throw new IllegalStateException("Stargate network is not running");
        }
        return connections;
    }

    /**
     * The connection manager, if a server is running and has finished starting.
     *
     * <p><i>FR —</i> Le gestionnaire de connexions, si un serveur tourne et a fini de démarrer.
     *
     * @return the manager, or empty / le gestionnaire, ou vide
     */
    public static Optional<ConnectionManager> running() {
        return Optional.ofNullable(connections);
    }

    /**
     * Registers a gate at {@code location} with a random address (or returns the one already there).
     *
     * <p><i>FR —</i> Enregistre une porte à {@code location} avec une adresse aléatoire (ou renvoie celle
     * qui s'y trouve déjà).
     *
     * @param server   the running server / le serveur en cours
     * @param location where the gate stands / l'emplacement de la porte
     * @return the gate record / l'enregistrement de la porte
     */
    public static GateRecord registerGate(MinecraftServer server, GlobalPos location) {
        return registry(server).register(location, server.overworld().getRandom());
    }

    /**
     * Unregisters a gate and collapses any wormhole it is part of.
     *
     * <p><i>FR —</i> Désenregistre une porte et effondre le trou de ver dont elle fait partie.
     *
     * @param server the running server / le serveur en cours
     * @param gate   the gate id / l'identifiant de la porte
     * @return the removed record, or empty if unknown / l'enregistrement retiré, ou vide s'il est inconnu
     */
    public static Optional<GateRecord> removeGate(MinecraftServer server, UUID gate) {
        if (connections != null) {
            connections.onGateRemoved(gate);
        }
        return registry(server).unregister(gate);
    }

    /**
     * Shortcut for {@code connections().dial(gate, address)}.
     *
     * <p><i>FR —</i> Raccourci pour {@code connections().dial(gate, address)}.
     *
     * @param gate    the dialing gate / la porte qui compose
     * @param address the address to dial / l'adresse à composer
     * @return the result / le résultat
     * @see ConnectionManager#dial(UUID, StargateAddress)
     */
    public static DialResult dial(UUID gate, StargateAddress address) {
        return connections().dial(gate, address);
    }

    /** Creates the manager once the config is loaded. / <i>FR —</i> Crée le gestionnaire une fois la config chargée. */
    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        StargateRegistry registry = registry(server);
        // Resolve through the registry on each call so later registrations are seen.
        // Passe par le registre à chaque appel pour voir les portes enregistrées ensuite.
        GateDirectory directory = new GateDirectory() {
            @Override
            public Optional<UUID> resolve(StargateAddress address) {
                return registry(server).resolve(address);
            }

            @Override
            public boolean exists(UUID gate) {
                return registry(server).exists(gate);
            }
        };
        connections = new ConnectionManager(directory, StargateConfig.timings());
        connections.setListener(new SyncListener());
        StargateMod.LOGGER.info("Stargate network started with {} gate(s)", registry.all().size());
    }

    /**
     * Advances every gate by one tick, runs the kawoosh and transport ({@link TransportHandler}), then pushes
     * changed gates to their block entities.
     *
     * <p><i>FR —</i> Fait avancer toutes les portes d'un tick, exécute le kawoosh et le transport
     * ({@link TransportHandler}), puis transmet les portes modifiées à leurs block entities.
     */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (connections != null) {
            connections.tick();
            TransportHandler.tick(event.getServer(), connections, registry(event.getServer()));
            flushDirtyGates(event.getServer());
        }
    }

    /** Drops all wormholes. / <i>FR —</i> Abandonne tous les trous de ver. */
    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        if (connections != null) {
            connections.clear();
        }
        connections = null;
        server = null;
        DIRTY_GATES.clear();
        TransportHandler.clear();
    }

    /**
     * Gates changed since the last flush. Batched so that a burst of events (e.g. the 7 chevrons an
     * incoming gate lights at once) sends one update per gate per tick.
     * <i>FR —</i> Portes modifiées depuis le dernier envoi. Regroupées pour qu'une rafale d'événements (par
     * exemple les 7 chevrons qu'une porte entrante allume d'un coup) n'envoie qu'une mise à jour par porte
     * et par tick.
     */
    private static final Set<UUID> DIRTY_GATES = new LinkedHashSet<>();

    /**
     * Refreshes the block entity of every changed gate whose chunk is loaded. Unloaded gates catch up when
     * their chunk loads ({@link StargateBlockEntity#onLoad()}).
     *
     * <p><i>FR —</i> Rafraîchit le block entity de chaque porte modifiée dont le chunk est chargé. Les
     * portes non chargées se remettent à jour au chargement de leur chunk
     * ({@link StargateBlockEntity#onLoad()}).
     */
    private static void flushDirtyGates(MinecraftServer server) {
        if (DIRTY_GATES.isEmpty()) {
            return;
        }
        StargateRegistry registry = registry(server);
        for (UUID id : List.copyOf(DIRTY_GATES)) {
            registry.get(id).ifPresent(record -> {
                ServerLevel level = server.getLevel(record.location().dimension());
                BlockPos pos = record.location().pos();
                if (level != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof StargateBlockEntity gate) {
                    gate.refreshFromNetwork(false);
                }
            });
        }
        DIRTY_GATES.clear();
    }

    /**
     * Marks touched gates for the next flush, and logs at debug level.
     *
     * <p><i>FR —</i> Marque les portes touchées pour le prochain envoi, et journalise en niveau debug.
     */
    /** Players this close to a gate see its dial failures. / <i>FR —</i> Les joueurs à cette distance voient les échecs de numérotation. */
    private static final int NOTIFY_RANGE = 24;

    /** Runs {@code action} at a registered, loaded gate. / <i>FR —</i> Exécute {@code action} sur une porte enregistrée et chargée. */
    private static void atGate(UUID gate, java.util.function.BiConsumer<ServerLevel, BlockPos> action) {
        if (server == null) {
            return;
        }
        registry(server).get(gate).ifPresent(record -> {
            ServerLevel level = server.getLevel(record.location().dimension());
            BlockPos pos = record.location().pos();
            if (level != null && level.isLoaded(pos)) {
                action.accept(level, pos);
            }
        });
    }

    private static final class SyncListener implements GateListener {
        @Override
        public void onStateChanged(UUID gate, WormholeState from, WormholeState to) {
            DIRTY_GATES.add(gate);
            StargateMod.LOGGER.debug("Gate {}: {} -> {}", gate, from, to);
        }

        @Override
        public void onChevronEncoded(UUID gate, int chevron, StargateSymbol symbol, boolean incoming) {
            DIRTY_GATES.add(gate);
            if (!incoming) {
                atGate(gate, (level, pos) -> level.playSound(null, pos.above(4), SoundEvents.IRON_TRAPDOOR_CLOSE,
                        SoundSource.BLOCKS, 1.2f, 0.55f));
            }
        }

        @Override
        public void onDialProgress(UUID gate) {
            DIRTY_GATES.add(gate);
        }

        @Override
        public void onDialFailed(UUID gate, DialResult reason) {
            DIRTY_GATES.add(gate);
            StargateMod.LOGGER.debug("Gate {}: dial failed ({})", gate, reason);
            // Failures can come long after the last key press (when the last chevron locks): tell players nearby.
            // Un échec peut arriver bien après la dernière touche (au dernier chevron) : prévenir les joueurs proches.
            Component message = Component.translatable("stargate.dial." + reason.name().toLowerCase(Locale.ROOT));
            atGate(gate, (level, pos) -> {
                for (ServerPlayer player : level.players()) {
                    if (player.blockPosition().closerThan(pos, NOTIFY_RANGE)) {
                        player.sendOverlayMessage(message);
                    }
                }
            });
        }

        @Override
        public void onConnectionOpened(StargateConnection connection) {
            StargateMod.LOGGER.debug("Wormhole open {} -> {}", connection.source(), connection.target());
        }

        @Override
        public void onConnectionClosed(StargateConnection connection, CloseReason reason) {
            DIRTY_GATES.add(connection.source());
            DIRTY_GATES.add(connection.target());
            StargateMod.LOGGER.debug("Wormhole closed {} -> {} ({})", connection.source(), connection.target(), reason);
        }
    }
}
