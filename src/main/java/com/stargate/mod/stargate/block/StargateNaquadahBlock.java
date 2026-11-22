package com.stargate.mod.stargate.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A naquadah block: the plain segments of the gate ring, between the chevrons (see {@link StargateFrame}).
 *
 * <p><i>FR —</i> Un bloc de naquadah : les segments simples de l'anneau de la porte, entre les chevrons (voir
 * {@link StargateFrame}).
 */
public class StargateNaquadahBlock extends StargateFrameBlock {
    /**
     * Creates the block.
     *
     * <p><i>FR —</i> Crée le bloc.
     *
     * @param properties block properties / propriétés du bloc
     */
    public StargateNaquadahBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
