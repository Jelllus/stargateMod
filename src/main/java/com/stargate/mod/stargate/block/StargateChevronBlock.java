package com.stargate.mod.stargate.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A chevron of the gate ring (see {@link StargateFrame}). No block entity: the control block drives
 * {@link #LIT} as symbols lock.
 *
 * <p><i>FR —</i> Un chevron de l'anneau de la porte (voir {@link StargateFrame}). Pas de block entity : c'est
 * le bloc de contrôle qui pilote {@link #LIT} au fil des symboles verrouillés.
 */
public class StargateChevronBlock extends StargateFrameBlock {
    /**
     * {@code true} while this chevron is locked.
     *
     * <p><i>FR —</i> {@code true} tant que ce chevron est verrouillé.
     */
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    /**
     * Creates the block.
     *
     * <p><i>FR —</i> Crée le bloc.
     *
     * @param properties block properties / propriétés du bloc
     */
    public StargateChevronBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }
}
