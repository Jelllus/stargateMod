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
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side entry point for gates: owns the {@link ConnectionManager} for the running server, ticks it,
 * and keeps it in step with the {@link StargateRegistry}.
 *
 * <p>Wormholes are not saved: they all close when the server stops.
 *
 * <p><i>FR —</i> Point d'entrée côté serveur pour les portes : possède le {@link ConnectionManager} du
 * serveur en cours, le fait avancer à chaque tick, et le garde en phase avec le {@link StargateRegistry}.
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
        connections.setListener(new LoggingListener());
        StargateMod.LOGGER.info("Stargate network started with {} gate(s)", registry.all().size());
    }

    /** Advances every gate by one tick. / <i>FR —</i> Fait avancer toutes les portes d'un tick. */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (connections != null) {
            connections.tick();
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
    }

    /**
     * Placeholder until gates have a block entity to drive visuals and sounds.
     *
     * <p><i>FR —</i> Solution provisoire en attendant que les portes aient un block entity pour l'affichage
     * et les sons.
     */
    private static final class LoggingListener implements GateListener {
        @Override
        public void onStateChanged(UUID gate, WormholeState from, WormholeState to) {
            StargateMod.LOGGER.debug("Gate {}: {} -> {}", gate, from, to);
        }

        @Override
        public void onDialFailed(UUID gate, DialResult reason) {
            StargateMod.LOGGER.debug("Gate {}: dial failed ({})", gate, reason);
        }

        @Override
        public void onConnectionOpened(StargateConnection connection) {
            StargateMod.LOGGER.debug("Wormhole open {} -> {}", connection.source(), connection.target());
        }

        @Override
        public void onConnectionClosed(StargateConnection connection, CloseReason reason) {
            StargateMod.LOGGER.debug("Wormhole closed {} -> {} ({})", connection.source(), connection.target(), reason);
        }
    }
}
