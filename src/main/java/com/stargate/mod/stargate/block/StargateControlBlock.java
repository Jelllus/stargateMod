package com.stargate.mod.stargate.block;

import com.stargate.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The control block: bottom middle of the frame (see {@link StargateFrame}), directly below the opening.
 * It holds the {@link StargateBlockEntity}, which registers the gate once the ring of
 * {@link StargateChevronBlock chevrons} and {@link StargateNaquadahBlock naquadah blocks} is complete and
 * unregisters it as soon as the frame is broken.
 *
 * <p>Once the ring is complete ({@link #FORMED}), this block draws the whole gate model and the other pieces
 * hide themselves. {@link #ACTIVE} mirrors whether a wormhole is linked (vortex forming or open): the event
 * horizon opens (kawoosh) and the block emits light. Right-clicking shows the gate status as the client sees it.
 *
 * <p><i>FR —</i> Le bloc de contrôle : en bas au milieu du cadre (voir {@link StargateFrame}), juste sous
 * l'ouverture. Il porte le {@link StargateBlockEntity}, qui enregistre la porte dès que l'anneau de
 * {@link StargateChevronBlock chevrons} et de {@link StargateNaquadahBlock blocs de naquadah} est complet et
 * la retire dès que le cadre est cassé.
 *
 * <p>Une fois l'anneau complet ({@link #FORMED}), ce bloc dessine le modèle complet de la porte et les autres
 * pièces se cachent. {@link #ACTIVE} reflète l'existence d'un trou de ver (vortex en formation ou ouvert) :
 * l'horizon des événements s'ouvre (kawoosh) et le bloc émet de la lumière. Un clic droit affiche l'état de la porte
 * tel que le client le voit.
 */
public class StargateControlBlock extends BaseEntityBlock {
    /**
     * {@code true} while the gate is {@code ENGAGING} or {@code OPEN}.
     *
     * <p><i>FR —</i> {@code true} tant que la porte est {@code ENGAGING} ou {@code OPEN}.
     */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /**
     * {@code true} while the ring is complete: the block then draws the gate model (ring and chevrons) in
     * place of its cube; the event horizon and the turning symbol track are drawn separately by
     * {@link com.stargate.mod.client.StargateRenderer}.
     *
     * <p><i>FR —</i> {@code true} tant que l'anneau est complet : le bloc dessine alors le modèle de la porte
     * (anneau et chevrons) à la place de son cube ; l'horizon des événements et la piste des symboles en rotation sont dessinés à part par
     * {@link com.stargate.mod.client.StargateRenderer}.
     */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    /**
     * Horizontal axis of the formed ring; picks the model rotation.
     *
     * <p><i>FR —</i> Axe horizontal de l'anneau formé ; détermine la rotation du modèle.
     */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    /**
     * Number of lit chevrons on the gate model, 0 to {@value StargateFrame#CHEVRONS}.
     *
     * <p><i>FR —</i> Nombre de chevrons allumés sur le modèle de la porte, de 0 à
     * {@value StargateFrame#CHEVRONS}.
     */
    public static final IntegerProperty CHEVRONS = IntegerProperty.create("chevrons", 0, StargateFrame.CHEVRONS);

    /**
     * {@code true} while the top chevron is locking a symbol the ring has just brought up: it lights.
     *
     * <p><i>FR —</i> {@code true} pendant que le chevron du haut verrouille un symbole que l'anneau vient
     * d'amener : il s'allume.
     */
    public static final BooleanProperty LOCKING = BooleanProperty.create("locking");

    /**
     * Creates the block.
     *
     * <p><i>FR —</i> Crée le bloc.
     *
     * @param properties block properties / propriétés du bloc
     */
    public StargateControlBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(ACTIVE, false)
                .setValue(FORMED, false)
                .setValue(AXIS, Direction.Axis.X)
                .setValue(CHEVRONS, 0)
                .setValue(LOCKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FORMED, AXIS, CHEVRONS, LOCKING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StargateBlockEntity(pos, state);
    }

    /**
     * Server-side ticker that re-checks the frame periodically.
     *
     * <p><i>FR —</i> Ticker côté serveur qui revérifie le cadre régulièrement.
     */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.STARGATE.get(), (l, pos, s, gate) -> gate.serverTick());
    }

    /**
     * Shows the synced gate status in the action bar (client side only).
     *
     * <p><i>FR —</i> Affiche l'état synchronisé de la porte dans la barre d'action (côté client uniquement).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof StargateBlockEntity gate) {
            player.sendOverlayMessage(gate.describe());
        }
        return InteractionResult.SUCCESS;
    }
}
