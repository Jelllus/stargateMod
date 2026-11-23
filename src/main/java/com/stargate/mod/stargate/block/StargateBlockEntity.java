package com.stargate.mod.stargate.block;

import com.stargate.mod.registry.ModBlockEntities;
import com.stargate.mod.stargate.address.StargateAddress;
import com.stargate.mod.stargate.address.StargateSymbol;
import com.stargate.mod.stargate.connection.ConnectionManager;
import com.stargate.mod.stargate.connection.RingMotion;
import com.stargate.mod.stargate.connection.StargateConnection;
import com.stargate.mod.stargate.connection.Timings;
import com.stargate.mod.stargate.connection.WormholeState;
import com.stargate.mod.stargate.network.StargateNetwork;
import com.stargate.mod.stargate.network.StargateRegistry;
import com.stargate.mod.stargate.network.StargateRegistry.GateRecord;
import com.stargate.mod.stargate.text.SymbolText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Block entity of the {@link StargateControlBlock}: decides whether the frame is a gate, ties it to its
 * {@link GateRecord}, and carries a copy of the gate's runtime state to clients.
 *
 * <h2>Server</h2>
 * <ul>
 *     <li>{@link #checkFrame()} looks for the 5×5 ring of chevrons and naquadah blocks
 *     ({@link StargateFrame}). When the frame
 *     completes, the gate is registered: the saved gate if it still matches this position, otherwise one
 *     already registered here, otherwise a new registration (keeping the last address when it is free).
 *     When the frame breaks, the gate is unregistered and its wormhole collapses; the address is kept so
 *     that rebuilding the frame gives it back. Called when a frame piece is placed or removed, on load, and every
 *     {@value #FRAME_CHECK_INTERVAL} ticks as a safety net.</li>
 *     <li>{@link #refreshFromNetwork(boolean)} copies state, chevrons and remote address from the
 *     {@link ConnectionManager}, updates the control block state ({@link StargateControlBlock#FORMED},
 *     {@link StargateControlBlock#AXIS}, {@link StargateControlBlock#CHEVRONS}, {@link StargateControlBlock#ACTIVE}),
 *     which selects the gate model, hides the frame pieces ({@link StargateFrameBlock#FORMED}), and sends
 *     the result to clients. {@link StargateNetwork} calls it whenever the gate changes.</li>
 *     <li>{@link #preRemoveSideEffects(BlockPos, BlockState)} unregisters the gate when the control block
 *     is broken or replaced.</li>
 * </ul>
 *
 * <h2>Client</h2>
 * Fields are filled from the update tag; read them with the getters or {@link #describe()}.
 *
 * <p>The runtime fields are saved too, but the server overwrites them from the network on load, so a
 * stale save never shows a wormhole that no longer exists.
 *
 * <p><i>FR —</i> Block entity du {@link StargateControlBlock} : décide si le cadre forme une porte, la relie
 * à son {@link GateRecord}, et transmet aux clients une copie de l'état d'exécution de la porte.
 *
 * <h2>Serveur</h2>
 * <ul>
 *     <li>{@link #checkFrame()} cherche l'anneau de 5×5 en chevrons et blocs de naquadah
 *     ({@link StargateFrame}). Quand le cadre est
 *     complet, la porte est enregistrée : la porte sauvegardée si elle correspond toujours à cette position,
 *     sinon une porte déjà enregistrée ici, sinon un nouvel enregistrement (en gardant la dernière adresse si
 *     elle est libre). Quand le cadre est cassé, la porte est désenregistrée et son trou de ver s'effondre ;
 *     l'adresse est conservée pour être rendue si le cadre est reconstruit.
 *     Appelé quand une pièce du cadre est posée ou retirée, au chargement, et toutes les
 *     {@value #FRAME_CHECK_INTERVAL} ticks par sécurité.</li>
 *     <li>{@link #refreshFromNetwork(boolean)} recopie l'état, les chevrons et l'adresse distante depuis le
 *     {@link ConnectionManager}, met à jour l'état du bloc de contrôle ({@link StargateControlBlock#FORMED},
 *     {@link StargateControlBlock#AXIS}, {@link StargateControlBlock#CHEVRONS}, {@link StargateControlBlock#ACTIVE}),
 *     qui choisit le modèle de la porte, cache les pièces du cadre ({@link StargateFrameBlock#FORMED}), et
 *     envoie le résultat aux clients. {@link StargateNetwork} l'appelle à chaque changement de la porte.</li>
 *     <li>{@link #preRemoveSideEffects(BlockPos, BlockState)} désenregistre la porte quand le bloc de
 *     contrôle est cassé ou remplacé.</li>
 * </ul>
 *
 * <h2>Client</h2>
 * Les champs sont remplis depuis le tag de mise à jour ; lisez-les avec les accesseurs ou
 * {@link #describe()}.
 *
 * <p>Les champs d'exécution sont aussi sauvegardés, mais le serveur les écrase depuis le réseau au
 * chargement : une sauvegarde périmée n'affiche jamais un trou de ver qui n'existe plus.
 */
public class StargateBlockEntity extends BlockEntity {
    /**
     * Ticks between two periodic frame checks.
     * <i>FR —</i> Ticks entre deux vérifications périodiques du cadre.
     */
    private static final int FRAME_CHECK_INTERVAL = 40;

    private @Nullable UUID gateId;
    private @Nullable StargateAddress address;
    private Direction.@Nullable Axis axis;
    private int framePieces;

    private WormholeState state = WormholeState.IDLE;
    private List<StargateSymbol> chevrons = List.of();
    private List<StargateSymbol> dialed = List.of();
    /** Ring movement, in game time. / <i>FR —</i> Mouvement de l'anneau, en temps de jeu. */
    private RingMotion ring = RingMotion.rest(0);
    private boolean incoming;
    private @Nullable StargateAddress remoteAddress;

    private int tickCounter;

    // Animation timing, sent to clients. / Minutage des animations, envoyé aux clients.
    private int engageTicks = Timings.DEFAULT.engageTicks();
    private int closeTicks = Timings.DEFAULT.closeTicks();
    /** Client: game time at which {@link #state} last changed. / <i>FR —</i> Client : temps de jeu du dernier changement de {@link #state}. */
    private long phaseStart = Long.MIN_VALUE / 2;
    /** Client: whether a first update was received (a gate already open when loaded does not replay the kawoosh). */
    private boolean clientSynced;

    /**
     * Creates the block entity; called by the game.
     *
     * <p><i>FR —</i> Crée le block entity ; appelé par le jeu.
     *
     * @param pos   block position / position du bloc
     * @param state block state / état du bloc
     */
    public StargateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STARGATE.get(), pos, state);
    }

    // ---------------------------------------------------------------- accessors / accesseurs

    /**
     * Registry id of the gate, while the frame is complete.
     *
     * <p><i>FR —</i> Identifiant de la porte dans le registre, tant que le cadre est complet.
     *
     * @return the id, or empty / l'identifiant, ou vide
     */
    public Optional<UUID> getGateId() {
        return Optional.ofNullable(gateId);
    }

    /**
     * Address of this gate (kept while the frame is broken, to be given back when it is rebuilt).
     *
     * <p><i>FR —</i> Adresse de cette porte (conservée pendant que le cadre est cassé, pour être rendue à sa
     * reconstruction).
     *
     * @return the address, or empty if the frame was never complete / l'adresse, ou vide si le cadre n'a
     *         jamais été complet
     */
    public Optional<StargateAddress> getAddress() {
        return Optional.ofNullable(address);
    }

    /**
     * Whether the whole ring is in place and the gate is registered.
     *
     * <p><i>FR —</i> Indique si l'anneau complet est en place et la porte enregistrée.
     *
     * @return {@code true} if formed / {@code true} si formée
     */
    public boolean isFormed() {
        return axis != null;
    }

    /**
     * Horizontal axis of the frame, while formed.
     *
     * <p><i>FR —</i> Axe horizontal du cadre, tant qu'il est formé.
     *
     * @return the axis, or empty / l'axe, ou vide
     */
    public Optional<Direction.Axis> getAxis() {
        return Optional.ofNullable(axis);
    }

    /**
     * Frame pieces (chevrons and naquadah blocks) correctly placed around this control block, 0 to
     * {@value StargateFrame#PIECES}.
     *
     * <p><i>FR —</i> Pièces du cadre (chevrons et blocs de naquadah) bien placées autour de ce bloc de
     * contrôle, de 0 à {@value StargateFrame#PIECES}.
     *
     * @return the count / le nombre
     */
    public int getFramePieces() {
        return framePieces;
    }

    /**
     * Last known gate state.
     *
     * <p><i>FR —</i> Dernier état connu de la porte.
     *
     * @return the state / l'état
     */
    public WormholeState getGateState() {
        return state;
    }

    /**
     * Symbols locked, in order (point of origin included once encoded).
     *
     * <p><i>FR —</i> Symboles verrouillés, dans l'ordre (point d'origine compris une fois encodé).
     *
     * @return an unmodifiable list / une liste non modifiable
     */
    public List<StargateSymbol> getChevrons() {
        return chevrons;
    }

    /**
     * Every symbol dialed, locked or still to be brought up by the ring (what a DHD shows as pressed).
     *
     * <p><i>FR —</i> Tous les symboles composés, verrouillés ou encore à amener par l'anneau (ce qu'un DHD
     * montre comme enfoncé).
     *
     * @return an unmodifiable list / une liste non modifiable
     */
    public List<StargateSymbol> getDialed() {
        return dialed;
    }

    /**
     * Client: symbol under the top chevron right now, fractional while the ring turns.
     *
     * <p><i>FR —</i> Client : symbole sous le chevron du haut en ce moment, fractionnaire pendant la
     * rotation de l'anneau.
     *
     * @param partialTick fraction of the current tick / fraction du tick en cours
     * @return position in [0, 39) / position dans [0, 39)
     */
    public double getRingPosition(float partialTick) {
        return level == null ? ring.to() : ring.position(level.getGameTime() + partialTick);
    }

    /**
     * Whether this gate is the dialed (incoming) end of its wormhole.
     *
     * <p><i>FR —</i> Indique si cette porte est l'extrémité appelée (entrante) de son trou de ver.
     *
     * @return {@code true} if incoming / {@code true} si entrante
     */
    public boolean isIncoming() {
        return incoming;
    }

    /**
     * Address of the gate at the other end, while connected.
     *
     * <p><i>FR —</i> Adresse de la porte à l'autre bout, pendant la connexion.
     *
     * @return the remote address, or empty / l'adresse distante, ou vide
     */
    public Optional<StargateAddress> getRemoteAddress() {
        return Optional.ofNullable(remoteAddress);
    }

    /**
     * Client: ticks since the gate entered its current state, for animations. Very large when the state was
     * already in place when the gate was loaded, so animations show their settled look.
     *
     * <p><i>FR —</i> Client : ticks écoulés depuis l'entrée de la porte dans son état actuel, pour les
     * animations. Très grand quand l'état était déjà en place au chargement de la porte, pour que les
     * animations montrent leur aspect stabilisé.
     *
     * @param partialTick fraction of the current tick / fraction du tick en cours
     * @return the age in ticks / l'âge en ticks
     */
    public float getPhaseAge(float partialTick) {
        if (level == null) {
            return Float.MAX_VALUE;
        }
        return (float) Math.min(1.0e6, level.getGameTime() - phaseStart) + partialTick;
    }

    /**
     * Duration of the vortex ({@link WormholeState#ENGAGING}), as configured on the server.
     *
     * <p><i>FR —</i> Durée du vortex ({@link WormholeState#ENGAGING}), telle que configurée sur le serveur.
     *
     * @return ticks
     */
    public int getEngageTicks() {
        return engageTicks;
    }

    /**
     * Duration of the closing ({@link WormholeState#CLOSING}), as configured on the server.
     *
     * <p><i>FR —</i> Durée de la fermeture ({@link WormholeState#CLOSING}), telle que configurée sur le
     * serveur.
     *
     * @return ticks
     */
    public int getCloseTicks() {
        return closeTicks;
    }

    /**
     * One-line status for the player, e.g. {@code Stargate 1-2-3-4-5-6 · Open · 7 chevron(s) · → 7-8-9-10-11-12},
     * or the frame progress while incomplete.
     *
     * <p><i>FR —</i> État sur une ligne pour le joueur, par exemple
     * {@code Porte 1-2-3-4-5-6 · Ouverte · 7 chevron(s) · → 7-8-9-10-11-12}, ou l'avancement du cadre tant
     * qu'il est incomplet.
     *
     * @return a translatable component / un composant traduisible
     */
    public Component describe() {
        if (axis == null || address == null) {
            return Component.translatable("stargate.gate.incomplete", framePieces, StargateFrame.PIECES);
        }
        MutableComponent text = Component.translatable("stargate.gate.status",
                SymbolText.address(address),
                Component.translatable("stargate.state." + state.name().toLowerCase(Locale.ROOT)),
                chevrons.size());
        if (remoteAddress != null) {
            text.append(Component.translatable(incoming ? "stargate.gate.incoming" : "stargate.gate.outgoing",
                    SymbolText.glyphs(remoteAddress)));
        }
        return text;
    }

    // ---------------------------------------------------------------- server: frame / serveur : cadre

    /**
     * Schedules a first frame check and sync to clients.
     *
     * <p><i>FR —</i> Programme une première vérification du cadre et une synchronisation vers les clients.
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            // Changing blocks while the chunk is loading is unsafe: do it on the next task.
            // Modifier des blocs pendant le chargement du chunk est risqué : on le fait à la tâche suivante.
            serverLevel.getServer().execute(() -> {
                if (!isRemoved()) {
                    checkFrame();
                    refreshFromNetwork(true);
                }
            });
        }
    }

    /**
     * Called every server tick by the control block's ticker.
     *
     * <p><i>FR —</i> Appelé à chaque tick serveur par le ticker du bloc de contrôle.
     */
    void serverTick() {
        if (++tickCounter >= FRAME_CHECK_INTERVAL) {
            tickCounter = 0;
            checkFrame();
        }
    }

    /**
     * Re-checks the frame and forms or breaks the gate accordingly. Does nothing if part of the frame is in
     * an unloaded chunk. Server side only.
     *
     * <p><i>FR —</i> Revérifie le cadre et forme ou casse la porte en conséquence. Ne fait rien si une partie
     * du cadre est dans un chunk non chargé. Côté serveur uniquement.
     */
    public void checkFrame() {
        if (!(level instanceof ServerLevel serverLevel) || isRemoved() || !StargateFrame.isLoaded(serverLevel, worldPosition)) {
            return;
        }
        int count = StargateFrame.bestPieceCount(serverLevel, worldPosition);
        Direction.Axis detected = StargateFrame.detect(serverLevel, worldPosition).orElse(null);
        boolean changed = count != framePieces;
        framePieces = count;

        boolean registered = gateId != null && StargateNetwork.registry(serverLevel.getServer()).exists(gateId);
        if (detected != null && (detected != axis || !registered)) {
            if (axis != null && detected != axis) {
                updateFramePieces(serverLevel, false, 0); // old frame / ancien cadre
            }
            axis = detected;
            bind(serverLevel);
            changed = true;
        } else if (detected == null && axis != null) {
            unbind(serverLevel);
            changed = true;
        }
        if (changed) {
            setChanged();
            refreshFromNetwork(true);
        }
    }

    private void bind(ServerLevel serverLevel) {
        StargateRegistry registry = StargateNetwork.registry(serverLevel.getServer());
        GlobalPos here = GlobalPos.of(serverLevel.dimension(), worldPosition);
        GateRecord record = Optional.ofNullable(gateId).flatMap(registry::get)
                .filter(r -> r.location().equals(here))
                .or(() -> registry.findAt(here))
                .or(() -> Optional.ofNullable(address).flatMap(a -> registry.register(here, a)))
                .orElseGet(() -> registry.register(here, serverLevel.getRandom()));
        gateId = record.id();
        address = record.address();
    }

    /**
     * Unregisters the gate and shows the frame pieces again; keeps the address.
     * <i>FR —</i> Désenregistre la porte et réaffiche les pièces du cadre ; garde l'adresse.
     */
    private void unbind(ServerLevel serverLevel) {
        removeFromRegistry(serverLevel, worldPosition);
        updateFramePieces(serverLevel, false, 0);
        gateId = null;
        axis = null;
    }

    private void removeFromRegistry(ServerLevel serverLevel, BlockPos pos) {
        if (gateId == null) {
            return;
        }
        GlobalPos here = GlobalPos.of(serverLevel.dimension(), pos);
        UUID id = gateId;
        StargateNetwork.registry(serverLevel.getServer()).get(id)
                .filter(r -> r.location().equals(here))
                .ifPresent(r -> StargateNetwork.removeGate(serverLevel.getServer(), id));
    }

    /**
     * Unregisters the gate (and collapses its wormhole) and shows the frame pieces again when the control
     * block is broken or replaced.
     *
     * <p><i>FR —</i> Désenregistre la porte (et effondre son trou de ver) et réaffiche les pièces du cadre
     * quand le bloc de contrôle est cassé ou remplacé.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel serverLevel) {
            removeFromRegistry(serverLevel, pos);
            updateFramePieces(serverLevel, false, 0);
        }
    }

    // ---------------------------------------------------------------- server: runtime state / serveur : état

    /**
     * Copies the gate's runtime state from the network and, if anything changed (or {@code force}), saves
     * it, updates the control and chevron blocks, and sends it to clients. Server side only.
     *
     * <p><i>FR —</i> Recopie l'état d'exécution de la porte depuis le réseau et, si quelque chose a changé
     * (ou si {@code force}), le sauvegarde, met à jour le bloc de contrôle et les chevrons, et l'envoie aux
     * clients. Côté serveur uniquement.
     *
     * @param force send to clients even if nothing changed / envoyer aux clients même sans changement
     */
    public void refreshFromNetwork(boolean force) {
        if (!(level instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        WormholeState newState = WormholeState.IDLE;
        List<StargateSymbol> newChevrons = List.of();
        List<StargateSymbol> newDialed = List.of();
        RingMotion newRing = RingMotion.rest(ring.position(serverLevel.getGameTime()));
        boolean locking = false;
        boolean newIncoming = false;
        StargateAddress newRemote = null;

        Optional<ConnectionManager> manager = StargateNetwork.running();
        if (manager.isPresent() && gateId != null) {
            ConnectionManager cm = manager.get();
            newState = cm.getState(gateId);
            newChevrons = cm.getEncoded(gateId);
            newDialed = cm.getDialed(gateId);
            cm.restoreRing(gateId, newRing.to());
            newRing = cm.getRing(gateId).shifted(serverLevel.getGameTime() - cm.currentTick());
            locking = cm.isLocking(gateId);
            Optional<StargateConnection> connection = cm.getConnection(gateId);
            if (connection.isPresent()) {
                StargateConnection c = connection.get();
                newIncoming = !c.isSource(gateId);
                newRemote = newIncoming
                        ? StargateNetwork.registry(serverLevel.getServer()).get(c.source()).map(GateRecord::address).orElse(null)
                        : c.address();
            }
        }

        boolean changed = newState != state || !newChevrons.equals(chevrons) || newIncoming != incoming
                || !Objects.equals(newRemote, remoteAddress) || !newDialed.equals(dialed) || !newRing.equals(ring)
                || getBlockState().getValueOrElse(StargateControlBlock.LOCKING, false) != locking;
        if (!changed && !force) {
            return;
        }
        state = newState;
        chevrons = newChevrons;
        dialed = newDialed;
        ring = newRing;
        incoming = newIncoming;
        remoteAddress = newRemote;
        setChanged();

        boolean formed = axis != null;
        int lit = formed ? StargateFrame.litChevrons(chevrons.size(), state.isConnected()) : 0;
        if (formed) {
            updateFramePieces(serverLevel, true, lit);
        }
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof StargateControlBlock) {
            BlockState wanted = blockState
                    .setValue(StargateControlBlock.ACTIVE, state.isConnected())
                    .setValue(StargateControlBlock.FORMED, formed)
                    .setValue(StargateControlBlock.AXIS, formed ? axis : blockState.getValue(StargateControlBlock.AXIS))
                    .setValue(StargateControlBlock.CHEVRONS, lit)
                    .setValue(StargateControlBlock.LOCKING, formed && locking);
            if (wanted != blockState) {
                blockState = wanted;
                serverLevel.setBlock(worldPosition, blockState, Block.UPDATE_ALL);
            }
        }
        serverLevel.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
    }

    /**
     * Hides ({@code formed}) or shows the 15 frame pieces, and lights chevrons 1..{@code lit}.
     * <i>FR —</i> Cache ({@code formed}) ou montre les 15 pièces du cadre, et allume les chevrons 1 à {@code lit}.
     */
    private void updateFramePieces(ServerLevel serverLevel, boolean formed, int lit) {
        if (axis == null) {
            return;
        }
        for (int i = 1; i <= StargateFrame.CHEVRONS; i++) {
            BlockPos pos = StargateFrame.chevronPos(worldPosition, axis, i);
            BlockState s = serverLevel.getBlockState(pos);
            if (s.getBlock() instanceof StargateChevronBlock) {
                setIfChanged(serverLevel, pos, s, s.setValue(StargateFrameBlock.FORMED, formed)
                        .setValue(StargateChevronBlock.LIT, i <= lit));
            }
        }
        for (BlockPos pos : StargateFrame.naquadahPositions(worldPosition, axis)) {
            BlockState s = serverLevel.getBlockState(pos);
            if (s.getBlock() instanceof StargateNaquadahBlock) {
                setIfChanged(serverLevel, pos, s, s.setValue(StargateFrameBlock.FORMED, formed));
            }
        }
    }

    private static void setIfChanged(ServerLevel serverLevel, BlockPos pos, BlockState current, BlockState wanted) {
        if (wanted != current) {
            serverLevel.setBlock(pos, wanted, Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------- save & sync / sauvegarde & synchro

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("gate_id", UUIDUtil.CODEC, gateId);
        output.storeNullable("address", StargateRegistry.ADDRESS_CODEC, address);
        output.storeNullable("axis", Direction.Axis.CODEC, axis);
        output.putInt("frame_pieces", framePieces);
        output.putString("state", state.name());
        output.putIntArray("chevrons", chevrons.stream().mapToInt(StargateSymbol::id).toArray());
        output.putBoolean("incoming", incoming);
        output.putIntArray("dialed", dialed.stream().mapToInt(StargateSymbol::id).toArray());
        output.putDouble("ring_from", ring.from());
        output.putDouble("ring_to", ring.to());
        output.putLong("ring_start", ring.startTick());
        output.putInt("ring_spin", ring.spinTicks());
        output.putInt("ring_lock", ring.lockTicks());
        Timings timings = StargateNetwork.running().map(ConnectionManager::timings).orElse(Timings.DEFAULT);
        output.putInt("engage_ticks", timings.engageTicks());
        output.putInt("close_ticks", timings.closeTicks());
        output.storeNullable("remote", StargateRegistry.ADDRESS_CODEC, remoteAddress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        gateId = input.read("gate_id", UUIDUtil.CODEC).orElse(null);
        address = input.read("address", StargateRegistry.ADDRESS_CODEC).orElse(null);
        axis = input.read("axis", Direction.Axis.CODEC).orElse(null);
        framePieces = input.getIntOr("frame_pieces", 0);
        WormholeState previous = state;
        state = input.getString("state").flatMap(StargateBlockEntity::parseState).orElse(WormholeState.IDLE);
        if (level != null && level.isClientSide()) {
            if (clientSynced && state != previous) {
                phaseStart = level.getGameTime();
            }
            clientSynced = true;
        }
        chevrons = input.getIntArray("chevrons")
                .map(ids -> Arrays.stream(ids).filter(StargateSymbol::isValidId).mapToObj(StargateSymbol::of).toList())
                .orElse(List.of());
        dialed = input.getIntArray("dialed")
                .map(ids -> Arrays.stream(ids).filter(StargateSymbol::isValidId).mapToObj(StargateSymbol::of).toList())
                .orElse(List.of());
        ring = new RingMotion(input.getDoubleOr("ring_from", 0), input.getDoubleOr("ring_to", 0),
                input.getLongOr("ring_start", 0), Math.max(0, input.getIntOr("ring_spin", 0)),
                Math.max(0, input.getIntOr("ring_lock", 0)));
        incoming = input.getBooleanOr("incoming", false);
        remoteAddress = input.read("remote", StargateRegistry.ADDRESS_CODEC).orElse(null);
        engageTicks = Math.max(1, input.getIntOr("engage_ticks", Timings.DEFAULT.engageTicks()));
        closeTicks = Math.max(1, input.getIntOr("close_ticks", Timings.DEFAULT.closeTicks()));
    }

    private static Optional<WormholeState> parseState(String name) {
        try {
            return Optional.of(WormholeState.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Sends the block entity data when the block is updated.
     *
     * <p><i>FR —</i> Envoie les données du block entity quand le bloc est mis à jour.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Data sent with the chunk and with each update: everything {@link #saveAdditional} writes.
     *
     * <p><i>FR —</i> Données envoyées avec le chunk et à chaque mise à jour : tout ce qu'écrit
     * {@link #saveAdditional}.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
