package com.stargate.mod.stargate.transport;

import com.stargate.mod.registry.ModDamageTypes;
import com.stargate.mod.stargate.StargateConfig;
import com.stargate.mod.stargate.block.StargateControlBlock;
import com.stargate.mod.stargate.block.StargateFrameBlock;
import com.stargate.mod.stargate.connection.ConnectionManager;
import com.stargate.mod.stargate.connection.EventHorizonShape;
import com.stargate.mod.stargate.connection.StargateConnection;
import com.stargate.mod.stargate.connection.WormholeState;
import com.stargate.mod.stargate.network.StargateRegistry;
import com.stargate.mod.stargate.transport.TeleportationManager.LocatedGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Decides, every server tick, what happens to entities at an event horizon:
 * <ul>
 *     <li><b>Vortex forming</b> ({@link WormholeState#ENGAGING}) — whatever is inside the kawoosh, at either
 *     gate, is vaporised: living entities take lethal {@code stargate:kawoosh} damage (creative players only
 *     if {@link StargateConfig#KILLS_CREATIVE}), other entities (items, arrows, boats…) are removed, and so
 *     are blocks ({@link StargateConfig#DESTROYS_BLOCKS}; unbreakable and gate blocks are spared). The
 *     lethal volume is the rendered one ({@link EventHorizonShape#kawooshProfile}).</li>
 *     <li><b>Open</b> ({@link WormholeState#OPEN}) — an entity whose centre crosses the outgoing gate's
 *     horizon from the front to the back, inside the ring, is sent to the other gate by
 *     {@link TeleportationManager}. Crossing the incoming gate, or the back of the outgoing gate, does
 *     nothing: travel is one-way.</li>
 * </ul>
 * Crossings are detected by remembering, per gate, which side of the horizon each nearby entity was on the
 * previous tick. Spectators and passengers are ignored (passengers travel with their mount).
 *
 * <p><i>FR —</i> Décide, à chaque tick serveur, du sort des entités au niveau d'un horizon des événements :
 * <ul>
 *     <li><b>Vortex en formation</b> ({@link WormholeState#ENGAGING}) — tout ce qui se trouve dans le
 *     kawoosh, sur l'une ou l'autre porte, est désintégré : les entités vivantes subissent des dégâts mortels
 *     {@code stargate:kawoosh} (les joueurs en créatif seulement si {@link StargateConfig#KILLS_CREATIVE}),
 *     les autres entités (objets, flèches, bateaux…) sont supprimées, ainsi que les blocs
 *     ({@link StargateConfig#DESTROYS_BLOCKS} ; les blocs incassables et les blocs de porte sont épargnés).
 *     Le volume mortel est celui qui est dessiné ({@link EventHorizonShape#kawooshProfile}).</li>
 *     <li><b>Ouverte</b> ({@link WormholeState#OPEN}) — une entité dont le centre traverse l'horizon de la
 *     porte sortante de l'avant vers l'arrière, à l'intérieur de l'anneau, est envoyée vers l'autre porte par
 *     {@link TeleportationManager}. Traverser la porte entrante, ou l'arrière de la porte sortante, ne fait
 *     rien : le voyage est à sens unique.</li>
 * </ul>
 * Les traversées sont détectées en mémorisant, par porte, de quel côté de l'horizon se trouvait chaque
 * entité proche au tick précédent. Les spectateurs et les passagers sont ignorés (les passagers voyagent
 * avec leur monture).
 */
public final class TransportHandler {
    /**
     * Depth scanned on each side of an open horizon, in blocks.
     * / <i>FR —</i> Profondeur examinée de chaque côté d'un horizon ouvert, en blocs.
     */
    private static final double SCAN_DEPTH = 2.0;

    /**
     * Damage dealt by the kawoosh: far above any health pool, but finite to keep damage maths sane.
     * / <i>FR —</i> Dégâts du kawoosh : bien au-delà de toute vie, mais finis pour garder des calculs sains.
     */
    private static final float LETHAL_DAMAGE = 1.0e6f;

    /**
     * Blocks closer than this to the horizon plane are left alone (the opening itself).
     * / <i>FR —</i> Les blocs plus proches que cela du plan de l'horizon sont épargnés (l'ouverture elle-même).
     */
    private static final double BLOCK_MIN_W = 0.5;

    /** Per outgoing gate: entity id → local w last tick. / <i>FR —</i> Par porte sortante : id d'entité → w local au tick précédent. */
    private static final Map<UUID, Map<Integer, Double>> LAST_SIDE = new HashMap<>();

    private TransportHandler() {
    }

    /**
     * Runs vaporisation and transport for every linked wormhole. Call once per server tick, after the
     * {@link ConnectionManager} has ticked.
     *
     * <p><i>FR —</i> Exécute la désintégration et le transport pour chaque trou de ver relié. À appeler une
     * fois par tick serveur, après le tick du {@link ConnectionManager}.
     *
     * @param server      the server / le serveur
     * @param connections the connection manager / le gestionnaire de connexions
     * @param registry    the gate registry / le registre des portes
     */
    public static void tick(MinecraftServer server, ConnectionManager connections, StargateRegistry registry) {
        Set<UUID> open = new HashSet<>();
        for (StargateConnection c : connections.getConnections()) {
            WormholeState state = connections.getState(c.source());
            if (state == WormholeState.ENGAGING) {
                long age = connections.getTicksInState(c.source()).orElse(0L);
                int engage = Math.max(1, connections.timings().engageTicks());
                float jet = EventHorizonShape.kawooshLength(age / (float) engage);
                if (jet > 0f) {
                    vaporize(server, registry, c.source(), jet);
                    vaporize(server, registry, c.target(), jet);
                }
            } else if (state == WormholeState.OPEN) {
                open.add(c.source());
                transport(server, registry, c);
            }
        }
        LAST_SIDE.keySet().retainAll(open);
    }

    /**
     * Forgets all tracking (server shutdown).
     *
     * <p><i>FR —</i> Oublie tout le suivi (arrêt du serveur).
     */
    public static void clear() {
        LAST_SIDE.clear();
    }

    private static Optional<LocatedGate> locate(MinecraftServer server, StargateRegistry registry, UUID gate, boolean forceLoad) {
        return registry.get(gate).flatMap(r -> TeleportationManager.locate(server, r, forceLoad));
    }

    private static void vaporize(MinecraftServer server, StargateRegistry registry, UUID gateId, float jet) {
        Optional<LocatedGate> located = locate(server, registry, gateId, false);
        if (located.isEmpty()) {
            return;
        }
        ServerLevel level = located.get().level();
        GateBasis basis = located.get().basis();
        double radius = EventHorizonShape.RADIUS;
        DamageSource source = level.damageSources().source(ModDamageTypes.KAWOOSH);
        for (Entity e : level.getEntities((Entity) null, box(basis, radius, -0.5, jet + 0.5), TransportHandler::isCandidate)) {
            Vec3 c = e.getBoundingBox().getCenter();
            double[] l = basis.toLocal(c.x, c.y, c.z);
            double rn = Math.hypot(l[0], l[1]) / radius;
            if (rn >= 1 || l[2] < -0.5 || l[2] > jet * EventHorizonShape.kawooshProfile((float) rn) + 0.3) {
                continue;
            }
            if (e instanceof Player player && player.isCreative()) {
                if (StargateConfig.KILLS_CREATIVE.getAsBoolean()) {
                    player.kill(level);
                }
            } else if (e instanceof LivingEntity living) {
                living.hurtServer(level, source, LETHAL_DAMAGE);
            } else if (!(e instanceof Player)) {
                e.discard();
            }
        }
        if (StargateConfig.DESTROYS_BLOCKS.getAsBoolean()) {
            vaporizeBlocks(level, basis, radius, jet);
        }
    }

    /**
     * Removes every block whose centre is inside the kawoosh, in front of the horizon. Unbreakable blocks
     * (bedrock…) and gate blocks are spared; containers lose their contents, nothing drops.
     * <i>FR —</i> Supprime chaque bloc dont le centre est dans le kawoosh, devant l'horizon. Les blocs
     * incassables (bedrock…) et les blocs de porte sont épargnés ; les conteneurs perdent leur contenu, rien
     * ne tombe.
     */
    private static void vaporizeBlocks(ServerLevel level, GateBasis basis, double radius, float jet) {
        AABB area = box(basis, radius, BLOCK_MIN_W, jet);
        BlockPos min = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos max = BlockPos.containing(area.maxX, area.maxY, area.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            double[] l = basis.toLocal(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double rn = Math.hypot(l[0], l[1]) / radius;
            if (rn >= 1 || l[2] < BLOCK_MIN_W || l[2] > jet * EventHorizonShape.kawooshProfile((float) rn)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0
                    || state.getBlock() instanceof StargateFrameBlock || state.getBlock() instanceof StargateControlBlock) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof Clearable container) {
                container.clearContent();
            }
            level.removeBlock(pos, false);
        }
    }

    private static void transport(MinecraftServer server, StargateRegistry registry, StargateConnection c) {
        Optional<LocatedGate> from = locate(server, registry, c.source(), false);
        if (from.isEmpty()) {
            return;
        }
        GateBasis basis = from.get().basis();
        double radius = EventHorizonShape.RADIUS;
        Map<Integer, Double> previous = LAST_SIDE.getOrDefault(c.source(), Map.of());
        Map<Integer, Double> now = new HashMap<>();
        Optional<LocatedGate> to = Optional.empty();

        List<Entity> nearby = from.get().level().getEntities((Entity) null,
                box(basis, radius, -SCAN_DEPTH, SCAN_DEPTH), TransportHandler::isCandidate);
        for (Entity e : nearby) {
            Vec3 centre = e.getBoundingBox().getCenter();
            double[] l = basis.toLocal(centre.x, centre.y, centre.z);
            boolean insideRing = Math.hypot(l[0], l[1]) < radius;
            Double before = previous.get(e.getId());
            if (insideRing && before != null && before > 0 && l[2] <= 0) {
                if (to.isEmpty()) {
                    to = locate(server, registry, c.target(), true);
                    if (to.isEmpty()) {
                        return; // destination gate gone / porte de destination disparue
                    }
                }
                TeleportationManager.teleport(e, from.get(), to.get());
            } else {
                now.put(e.getId(), l[2]);
            }
        }
        LAST_SIDE.put(c.source(), now);
    }

    private static boolean isCandidate(Entity e) {
        return e.isAlive() && !e.isSpectator() && !e.isPassenger();
    }

    /** World box around the horizon, from {@code wMin} to {@code wMax} along the normal. / <i>FR —</i> Boîte monde autour de l'horizon, de {@code wMin} à {@code wMax} le long de la normale. */
    private static AABB box(GateBasis basis, double radius, double wMin, double wMax) {
        double[] a = basis.toWorld(-radius, -radius, wMin);
        double[] b = basis.toWorld(radius, radius, wMax);
        return new AABB(a[0], a[1], a[2], b[0], b[1], b[2]);
    }
}
