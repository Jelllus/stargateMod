package com.stargate.mod.stargate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shape of a gate: a 5×5 ring in a vertical plane along the X or Z axis, around a 3×3 opening. Seen from
 * the front ({@code K} = control, {@code N} = naquadah block, digits = chevron order):
 *
 * <pre>
 *  6 N 7 N 1
 *  N . . . N
 *  5 . . . 2
 *  N . . . N
 *  4 N K N 3
 * </pre>
 *
 * Seven chevrons, eight naquadah blocks and the control block: {@value #PIECES} pieces around the control.
 * Chevrons are numbered clockwise from the top right; 1 to 6 light as symbols are encoded, 7 (top middle)
 * locks with the point of origin.
 *
 * <p><i>FR —</i> Forme d'une porte : un anneau de 5×5 dans un plan vertical orienté selon l'axe X ou Z,
 * autour d'une ouverture de 3×3. Vue de face ({@code K} = contrôle, {@code N} = bloc de naquadah, chiffres =
 * ordre des chevrons) : voir le schéma ci-dessus.
 *
 * <p>Sept chevrons, huit blocs de naquadah et le bloc de contrôle : {@value #PIECES} pièces autour du
 * contrôle. Les chevrons sont numérotés dans le sens horaire depuis le haut à droite ; 1 à 6 s'allument au
 * fil des symboles encodés, 7 (en haut au milieu) se verrouille avec le point d'origine.
 */
public final class StargateFrame {
    /**
     * Number of chevron blocks. / <i>FR —</i> Nombre de blocs chevrons.
     */
    public static final int CHEVRONS = 7;

    /**
     * Number of frame pieces around the control block (chevrons + naquadah).
     * / <i>FR —</i> Nombre de pièces du cadre autour du contrôle (chevrons + naquadah).
     */
    public static final int PIECES = 15;

    /** {sideways, up} offset of chevrons 1..7 from the control block. / <i>FR —</i> Décalage {côté, haut} des chevrons 1 à 7 par rapport au contrôle. */
    private static final int[][] CHEVRON_OFFSETS = {
            {2, 4}, {2, 2}, {2, 0}, {-2, 0}, {-2, 2}, {-2, 4}, {0, 4}
    };

    /** {sideways, up} offset of the naquadah blocks. / <i>FR —</i> Décalage {côté, haut} des blocs de naquadah. */
    private static final int[][] NAQUADAH_OFFSETS = {
            {-1, 4}, {1, 4}, {-2, 3}, {2, 3}, {-2, 1}, {2, 1}, {-1, 0}, {1, 0}
    };

    private static final Direction.Axis[] AXES = {Direction.Axis.X, Direction.Axis.Z};

    private StargateFrame() {
    }

    private static BlockPos at(BlockPos control, Direction.Axis axis, int[] o) {
        return axis == Direction.Axis.X ? control.offset(o[0], o[1], 0) : control.offset(0, o[1], o[0]);
    }

    /**
     * Position of a chevron.
     *
     * <p><i>FR —</i> Position d'un chevron.
     *
     * @param control control block position / position du bloc de contrôle
     * @param axis    horizontal axis of the frame / axe horizontal du cadre
     * @param chevron chevron number, 1 to {@link #CHEVRONS} / numéro du chevron, de 1 à {@link #CHEVRONS}
     * @return the chevron position / la position du chevron
     */
    public static BlockPos chevronPos(BlockPos control, Direction.Axis axis, int chevron) {
        return at(control, axis, CHEVRON_OFFSETS[chevron - 1]);
    }

    /**
     * Positions of the eight naquadah blocks.
     *
     * <p><i>FR —</i> Positions des huit blocs de naquadah.
     *
     * @param control control block position / position du bloc de contrôle
     * @param axis    horizontal axis of the frame / axe horizontal du cadre
     * @return the positions / les positions
     */
    public static List<BlockPos> naquadahPositions(BlockPos control, Direction.Axis axis) {
        List<BlockPos> out = new ArrayList<>(NAQUADAH_OFFSETS.length);
        for (int[] o : NAQUADAH_OFFSETS) {
            out.add(at(control, axis, o));
        }
        return out;
    }

    /**
     * The nine blocks of the 3×3 opening, row by row from the bottom.
     *
     * <p><i>FR —</i> Les neuf blocs de l'ouverture de 3×3, rangée par rangée en partant du bas.
     *
     * @param control control block position / position du bloc de contrôle
     * @param axis    horizontal axis of the frame / axe horizontal du cadre
     * @return the opening positions / les positions de l'ouverture
     */
    public static List<BlockPos> opening(BlockPos control, Direction.Axis axis) {
        List<BlockPos> out = new ArrayList<>(9);
        for (int up = 1; up <= 3; up++) {
            for (int side = -1; side <= 1; side++) {
                out.add(at(control, axis, new int[]{side, up}));
            }
        }
        return out;
    }

    /**
     * Finds the axis along which the whole frame stands around {@code control}.
     *
     * <p><i>FR —</i> Cherche l'axe selon lequel le cadre complet entoure {@code control}.
     *
     * @param level   the level / le niveau
     * @param control control block position / position du bloc de contrôle
     * @return the axis, or empty if the frame is incomplete / l'axe, ou vide si le cadre est incomplet
     */
    public static Optional<Direction.Axis> detect(Level level, BlockPos control) {
        for (Direction.Axis axis : AXES) {
            if (countPieces(level, control, axis) == PIECES) {
                return Optional.of(axis);
            }
        }
        return Optional.empty();
    }

    /**
     * Highest number of correct pieces over both axes, to tell the player how far along the frame is.
     *
     * <p><i>FR —</i> Nombre maximal de pièces correctes sur les deux axes, pour indiquer au joueur où en est
     * le cadre.
     *
     * @param level   the level / le niveau
     * @param control control block position / position du bloc de contrôle
     * @return 0 to {@link #PIECES}
     */
    public static int bestPieceCount(Level level, BlockPos control) {
        int best = 0;
        for (Direction.Axis axis : AXES) {
            best = Math.max(best, countPieces(level, control, axis));
        }
        return best;
    }

    private static int countPieces(Level level, BlockPos control, Direction.Axis axis) {
        int n = 0;
        for (int[] o : CHEVRON_OFFSETS) {
            if (level.getBlockState(at(control, axis, o)).getBlock() instanceof StargateChevronBlock) {
                n++;
            }
        }
        for (int[] o : NAQUADAH_OFFSETS) {
            if (level.getBlockState(at(control, axis, o)).getBlock() instanceof StargateNaquadahBlock) {
                n++;
            }
        }
        return n;
    }

    /**
     * Whether every block of the frame (both axes) is in a loaded chunk, so {@link #detect} can be trusted.
     *
     * <p><i>FR —</i> Indique si tous les blocs du cadre (sur les deux axes) sont dans des chunks chargés,
     * pour que {@link #detect} soit fiable.
     *
     * @param level   the level / le niveau
     * @param control control block position / position du bloc de contrôle
     * @return {@code true} if all loaded / {@code true} si tout est chargé
     */
    public static boolean isLoaded(Level level, BlockPos control) {
        // The frame spans ±2 sideways and 0..4 up: checking the four corners covers every chunk.
        // Le cadre s'étend sur ±2 de côté et 0 à 4 en hauteur : vérifier les quatre coins couvre tous les chunks.
        return level.isLoaded(control.offset(-2, 0, -2)) && level.isLoaded(control.offset(2, 0, -2))
                && level.isLoaded(control.offset(-2, 0, 2)) && level.isLoaded(control.offset(2, 0, 2));
    }

    /**
     * Positions where a control block would have to be for a frame piece at {@code piece} to belong to its
     * frame, whatever the piece (chevron or naquadah).
     *
     * <p><i>FR —</i> Positions où un bloc de contrôle devrait se trouver pour qu'une pièce du cadre placée à
     * {@code piece} fasse partie de son cadre, quelle que soit la pièce (chevron ou naquadah).
     *
     * @param piece a frame piece position / la position d'une pièce du cadre
     * @return candidate control positions / positions candidates du contrôle
     */
    public static List<BlockPos> candidateControls(BlockPos piece) {
        List<BlockPos> out = new ArrayList<>((CHEVRON_OFFSETS.length + NAQUADAH_OFFSETS.length) * AXES.length);
        for (int[][] offsets : new int[][][]{CHEVRON_OFFSETS, NAQUADAH_OFFSETS}) {
            for (int[] o : offsets) {
                out.add(piece.offset(-o[0], -o[1], 0));
                out.add(piece.offset(0, -o[1], -o[0]));
            }
        }
        return out;
    }

    /**
     * How many of the 7 chevrons are lit for a dial in progress.
     *
     * <p>Chevrons 1–6 follow the encoded symbols; chevron 7 lights once the 7th symbol or the point of
     * origin is in. A linked gate shows all seven.
     *
     * <p><i>FR —</i> Nombre de chevrons allumés sur les 7 pour une numérotation en cours.
     *
     * <p>Les chevrons 1 à 6 suivent les symboles encodés ; le chevron 7 s'allume dès que le 7e symbole ou le
     * point d'origine est entré. Une porte reliée les montre tous les sept.
     *
     * @param encodedCount symbols encoded, point of origin included / symboles encodés, point d'origine
     *                     compris
     * @param connected    whether a wormhole is linked / indique si un trou de ver est relié
     * @return 0 to {@link #CHEVRONS}
     */
    public static int litChevrons(int encodedCount, boolean connected) {
        if (connected) {
            return CHEVRONS;
        }
        return Math.min(encodedCount, CHEVRONS);
    }
}
