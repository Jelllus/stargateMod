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
 */
@EventBusSubscriber(modid = StargateMod.MODID)
public final class StargateNetwork {
    private static MinecraftServer server;
    private static ConnectionManager connections;

    private StargateNetwork() {
    }

    public static StargateRegistry registry(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(StargateRegistry.TYPE);
    }

    /** @throws IllegalStateException if no server is running */
    public static ConnectionManager connections() {
        if (connections == null) {
            throw new IllegalStateException("Stargate network is not running");
        }
        return connections;
    }

    public static GateRecord registerGate(MinecraftServer server, GlobalPos location) {
        return registry(server).register(location, server.overworld().getRandom());
    }

    /** Unregisters a gate and collapses any wormhole it is part of. */
    public static Optional<GateRecord> removeGate(MinecraftServer server, UUID gate) {
        if (connections != null) {
            connections.onGateRemoved(gate);
        }
        return registry(server).unregister(gate);
    }

    public static DialResult dial(UUID gate, StargateAddress address) {
        return connections().dial(gate, address);
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        StargateRegistry registry = registry(server);
        // Resolve through the registry on each call so later registrations are seen.
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

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (connections != null) {
            connections.tick();
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        if (connections != null) {
            connections.clear();
        }
        connections = null;
        server = null;
    }

    /** Placeholder until gates have a block entity to drive visuals and sounds. */
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
