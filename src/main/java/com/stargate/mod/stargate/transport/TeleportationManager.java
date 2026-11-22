package com.stargate.mod.stargate.transport;

import com.stargate.mod.stargate.block.StargateBlockEntity;
import com.stargate.mod.stargate.network.StargateRegistry.GateRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Moves entities between gates: from the event horizon of an open outgoing gate to the front of the gate it
 * is linked to, across dimensions if needed. Position in the ring, speed and facing are carried over (see
 * {@link GateBasis#transfer}); riders travel with their mount.
 *
 * <p><i>FR —</i> Déplace les entités entre les portes : de l'horizon des événements d'une porte sortante
 * ouverte vers l'avant de la porte à laquelle elle est reliée, y compris vers une autre dimension. La
 * position dans l'anneau, la vitesse et l'orientation sont conservées (voir {@link GateBasis#transfer}) ;
 * les passagers voyagent avec leur monture.
 */
public final class TeleportationManager {
    /**
     * How far in front of the target horizon travellers appear, in blocks.
     * / <i>FR —</i> Distance devant l'horizon cible où apparaissent les voyageurs, en blocs.
     */
    public static final double EXIT_PUSH = 0.6;

    private TeleportationManager() {
    }

    /**
     * A gate located in the world: its level and the frame of its horizon.
     *
     * <p><i>FR —</i> Une porte située dans le monde : son niveau et le repère de son horizon.
     *
     * @param level the gate's level / le niveau de la porte
     * @param basis the horizon frame / le repère de l'horizon
     */
    public record LocatedGate(ServerLevel level, GateBasis basis) {
    }

    /**
     * Finds where a registered gate stands and how it is oriented. Needs its block entity, so the chunk must
     * be loaded, or {@code forceLoad} must be set (used for the destination).
     *
     * <p><i>FR —</i> Trouve où se trouve une porte enregistrée et comment elle est orientée. Il faut son block
     * entity : le chunk doit donc être chargé, ou {@code forceLoad} activé (utilisé pour la destination).
     *
     * @param server    the server / le serveur
     * @param record    the gate / la porte
     * @param forceLoad load the chunk if needed / charger le chunk si nécessaire
     * @return the located gate, or empty if unloaded or not formed / la porte située, ou vide si non chargée
     *         ou non formée
     */
    public static Optional<LocatedGate> locate(MinecraftServer server, GateRecord record, boolean forceLoad) {
        ServerLevel level = server.getLevel(record.location().dimension());
        BlockPos pos = record.location().pos();
        if (level == null || (!forceLoad && !level.isLoaded(pos))) {
            return Optional.empty();
        }
        if (forceLoad) {
            level.getChunkAt(pos);
        }
        if (!(level.getBlockEntity(pos) instanceof StargateBlockEntity gate)) {
            return Optional.empty();
        }
        return gate.getAxis().map(axis ->
                new LocatedGate(level, GateBasis.of(pos.getX(), pos.getY(), pos.getZ(), axis == Direction.Axis.X)));
    }

    /**
     * Sends {@code entity} through {@code from} to {@code to}.
     *
     * <p><i>FR —</i> Envoie {@code entity} à travers {@code from} vers {@code to}.
     *
     * @param entity the traveller (not a passenger) / le voyageur (pas un passager)
     * @param from   source gate / porte source
     * @param to     destination gate / porte de destination
     * @return the entity after the teleport (a new instance after a dimension change), or {@code null} if it
     *         failed / l'entité après la téléportation (une nouvelle instance après un changement de
     *         dimension), ou {@code null} en cas d'échec
     */
    public static Entity teleport(Entity entity, LocatedGate from, LocatedGate to) {
        Vec3 motion = entity.getDeltaMovement();
        GateBasis.Exit exit = GateBasis.transfer(from.basis(), to.basis(),
                new double[]{entity.getX(), entity.getY(), entity.getZ()},
                new double[]{motion.x, motion.y, motion.z},
                entity.getYRot(), EXIT_PUSH);
        return entity.teleport(new TeleportTransition(to.level(),
                new Vec3(exit.pos()[0], exit.pos()[1], exit.pos()[2]),
                new Vec3(exit.motion()[0], exit.motion()[1], exit.motion()[2]),
                exit.yaw(), entity.getXRot(), TeleportTransition.DO_NOTHING));
    }
}
