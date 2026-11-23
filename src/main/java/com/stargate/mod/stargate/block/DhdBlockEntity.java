package com.stargate.mod.stargate.block;

import com.stargate.mod.registry.ModBlockEntities;
import com.stargate.mod.stargate.network.StargateNetwork;
import com.stargate.mod.stargate.network.StargateRegistry.GateRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * Block entity of the {@link StargateDhdBlock}: remembers which gate the DHD dials, and shares it with clients
 * so the renderer can light the keys from that gate's state.
 *
 * <p>The DHD links to the nearest formed gate in the same dimension within {@value #LINK_RANGE} blocks (measured
 * to the control block). The link is made when the DHD loads and re-checked on every key press, so building a
 * gate after the DHD, or breaking the linked one, just works.
 *
 * <p><i>FR —</i> Block entity du {@link StargateDhdBlock} : retient quelle porte le DHD compose, et la partage
 * avec les clients pour que le rendu allume les touches selon l'état de cette porte.
 *
 * <p>Le DHD se relie à la porte formée la plus proche dans la même dimension, à {@value #LINK_RANGE} blocs au
 * plus (mesuré jusqu'au bloc de contrôle). Le lien est établi au chargement du DHD et revérifié à chaque appui
 * sur une touche : construire une porte après le DHD, ou casser la porte reliée, fonctionne sans rien faire.
 */
public class DhdBlockEntity extends BlockEntity {
    /**
     * Maximum distance to the gate's control block, in blocks.
     * / <i>FR —</i> Distance maximale jusqu'au bloc de contrôle de la porte, en blocs.
     */
    public static final int LINK_RANGE = 16;

    private @Nullable BlockPos linkedGate;

    /**
     * Creates the block entity; called by the game.
     *
     * <p><i>FR —</i> Crée le block entity ; appelé par le jeu.
     *
     * @param pos   block position / position du bloc
     * @param state block state / état du bloc
     */
    public DhdBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DHD.get(), pos, state);
    }

    /**
     * Position of the linked gate's control block (known on both sides).
     *
     * <p><i>FR —</i> Position du bloc de contrôle de la porte reliée (connue des deux côtés).
     *
     * @return the position, or empty / la position, ou vide
     */
    public Optional<BlockPos> getLinkedGate() {
        return Optional.ofNullable(linkedGate);
    }

    /**
     * The linked gate, re-linking first if it is missing or no longer formed. Server side.
     *
     * <p><i>FR —</i> La porte reliée, en refaisant le lien si elle manque ou n'est plus formée. Côté serveur.
     *
     * @return the gate's block entity, or empty if no gate is in range / le block entity de la porte, ou vide si
     *         aucune porte n'est à portée
     */
    public Optional<StargateBlockEntity> findGate() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        Optional<StargateBlockEntity> current = gateAt(serverLevel, linkedGate);
        if (current.isPresent()) {
            return current;
        }
        link(serverLevel);
        return gateAt(serverLevel, linkedGate);
    }

    private static Optional<StargateBlockEntity> gateAt(ServerLevel level, @Nullable BlockPos pos) {
        if (pos == null || !level.isLoaded(pos)) {
            return Optional.empty();
        }
        return level.getBlockEntity(pos) instanceof StargateBlockEntity gate && gate.isFormed() && gate.getGateId().isPresent()
                ? Optional.of(gate) : Optional.empty();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                if (!isRemoved()) {
                    link(serverLevel);
                }
            });
        }
    }

    /** Links to the nearest registered gate in range, and syncs if it changed. / <i>FR —</i> Se relie à la porte enregistrée la plus proche à portée, et synchronise si ça change. */
    private void link(ServerLevel serverLevel) {
        BlockPos found = StargateNetwork.registry(serverLevel.getServer()).all().stream()
                .filter(r -> r.location().dimension().equals(serverLevel.dimension()))
                .map(GateRecord::location)
                .map(l -> l.pos())
                .filter(p -> p.distSqr(worldPosition) <= (double) LINK_RANGE * LINK_RANGE)
                .min(Comparator.comparingDouble(p -> p.distSqr(worldPosition)))
                .orElse(null);
        if (!Objects.equals(found, linkedGate)) {
            linkedGate = found;
            setChanged();
            BlockState state = getBlockState();
            serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("linked_gate", BlockPos.CODEC, linkedGate);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        linkedGate = input.read("linked_gate", BlockPos.CODEC).orElse(null);
    }

    /**
     * Sends the link to clients when it changes.
     *
     * <p><i>FR —</i> Envoie le lien aux clients quand il change.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Data sent with the chunk and each update.
     *
     * <p><i>FR —</i> Données envoyées avec le chunk et à chaque mise à jour.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
