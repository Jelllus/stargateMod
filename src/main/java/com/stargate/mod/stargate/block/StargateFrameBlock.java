package com.stargate.mod.stargate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Base of the blocks that make up a gate ring around the control block ({@link StargateChevronBlock},
 * {@link StargateNaquadahBlock}). Placing or removing one asks every control block that could own it to
 * re-check its frame, so the gate forms or breaks right away.
 *
 * <p><i>FR —</i> Base des blocs qui composent l'anneau d'une porte autour du bloc de contrôle
 * ({@link StargateChevronBlock}, {@link StargateNaquadahBlock}). Poser ou retirer l'un d'eux demande à chaque
 * bloc de contrôle qui pourrait le posséder de revérifier son cadre : la porte se forme ou se casse
 * immédiatement.
 */
public abstract class StargateFrameBlock extends Block {
    /**
     * {@code true} while the piece belongs to a formed gate: the block is then hidden (empty model) and the
     * control block draws the whole gate.
     *
     * <p><i>FR —</i> {@code true} tant que la pièce appartient à une porte formée : le bloc est alors caché
     * (modèle vide) et c'est le bloc de contrôle qui dessine toute la porte.
     */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    /**
     * Creates the block.
     *
     * <p><i>FR —</i> Crée le bloc.
     *
     * @param properties block properties / propriétés du bloc
     */
    protected StargateFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    /**
     * A new piece may complete a frame. Ignored when only the block state changes (e.g. a chevron lighting).
     *
     * <p><i>FR —</i> Une nouvelle pièce peut compléter un cadre. Ignoré quand seul l'état du bloc change (par
     * exemple un chevron qui s'allume).
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            notifyControls(serverLevel, pos);
        }
    }

    /**
     * A removed piece breaks the frame it belonged to.
     *
     * <p><i>FR —</i> Une pièce retirée casse le cadre auquel elle appartenait.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        notifyControls(level, pos);
    }

    private static void notifyControls(ServerLevel level, BlockPos piece) {
        for (BlockPos candidate : StargateFrame.candidateControls(piece)) {
            if (level.isLoaded(candidate) && level.getBlockEntity(candidate) instanceof StargateBlockEntity gate) {
                gate.checkFrame();
            }
        }
    }
}
