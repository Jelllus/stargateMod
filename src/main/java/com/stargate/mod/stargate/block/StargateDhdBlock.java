package com.stargate.mod.stargate.block;

import com.stargate.mod.stargate.address.StargateSymbol;
import com.stargate.mod.stargate.connection.ConnectionManager;
import com.stargate.mod.stargate.connection.DialResult;
import com.stargate.mod.stargate.dhd.DhdLayout;
import com.stargate.mod.stargate.network.StargateNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The Dial Home Device: a pedestal with a tilted console of 38 keys, one per symbol, around the red dome
 * (point of origin). Placed facing the player. Dials the nearest gate (see {@link DhdBlockEntity}).
 *
 * <h2>Use</h2>
 * Right-click a key on the console to encode its symbol on the linked gate; the key lights up. Right-click the
 * dome to lock (the dome lights up and the wormhole forms); right-click it again while connected to shut the
 * wormhole. The pressed key is found by casting the player's look onto the console ({@link DhdLayout#pick}).
 * Failures (unknown address, gate busy…) are shown in the action bar.
 *
 * <p><i>FR —</i> Le DHD (Dial Home Device) : un pied surmonté d'une console inclinée de 38 touches, une par
 * symbole, autour du dôme rouge (point d'origine). Il est posé face au joueur et compose la porte la plus
 * proche (voir {@link DhdBlockEntity}).
 *
 * <h2>Utilisation</h2>
 * Clic droit sur une touche de la console pour encoder son symbole sur la porte reliée ; la touche s'allume.
 * Clic droit sur le dôme pour verrouiller (le dôme s'allume et le trou de ver se forme) ; nouveau clic droit
 * pendant la connexion pour fermer le trou de ver. La touche appuyée est trouvée en projetant le regard du
 * joueur sur la console ({@link DhdLayout#pick}). Les échecs (adresse inconnue, porte occupée…) s'affichent
 * dans la barre d'action.
 */
public class StargateDhdBlock extends BaseEntityBlock {
    /**
     * Direction the console faces (towards the operator).
     *
     * <p><i>FR —</i> Direction vers laquelle la console est tournée (vers l'opérateur).
     */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Pedestal, then the console. / <i>FR —</i> Le pied, puis la console. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 11, 12),
            Block.box(0, 10, 0, 16, 17, 16));

    /**
     * Creates the block.
     *
     * <p><i>FR —</i> Crée le bloc.
     *
     * @param properties block properties / propriétés du bloc
     */
    public StargateDhdBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DhdBlockEntity(pos, state);
    }

    /**
     * Faces the player who places it.
     *
     * <p><i>FR —</i> S'oriente face au joueur qui le pose.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /**
     * Presses the key the player is looking at.
     *
     * <p><i>FR —</i> Appuie sur la touche que le joueur regarde.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Direction facing = state.getValue(FACING);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int key = DhdLayout.pick(new double[]{eye.x - pos.getX(), eye.y - pos.getY(), eye.z - pos.getZ()},
                new double[]{look.x, look.y, look.z}, facing.getStepX(), facing.getStepZ());
        if (key == DhdLayout.NONE) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof DhdBlockEntity dhd) {
            press(serverLevel, pos, player, dhd, key);
        }
        return InteractionResult.SUCCESS;
    }

    private static void press(ServerLevel level, BlockPos pos, Player player, DhdBlockEntity dhd, int key) {
        Optional<StargateBlockEntity> gate = dhd.findGate();
        Optional<ConnectionManager> manager = StargateNetwork.running();
        if (gate.isEmpty() || manager.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("stargate.dhd.no_gate"));
            return;
        }
        UUID gateId = gate.get().getGateId().orElseThrow();
        ConnectionManager cm = manager.get();

        if (key == DhdLayout.DOME && cm.getState(gateId).isConnected()) {
            if (cm.disconnect(gateId)) {
                level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1f, 1f);
            } else {
                player.sendOverlayMessage(Component.translatable("stargate.dhd.incoming"));
            }
            return;
        }
        DialResult result = cm.encode(gateId, StargateSymbol.of(key));
        if (result.isSuccess()) {
            boolean dome = key == DhdLayout.DOME;
            level.playSound(null, pos, dome ? SoundEvents.BEACON_ACTIVATE : SoundEvents.STONE_BUTTON_CLICK_ON,
                    SoundSource.BLOCKS, dome ? 1f : 0.6f, dome ? 1f : 1.2f);
        } else {
            player.sendOverlayMessage(Component.translatable("stargate.dial." + result.name().toLowerCase(Locale.ROOT)));
        }
    }
}
