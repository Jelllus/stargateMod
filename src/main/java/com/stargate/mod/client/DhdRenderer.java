package com.stargate.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.address.StargateSymbol;
import com.stargate.mod.stargate.block.DhdBlockEntity;
import com.stargate.mod.stargate.block.StargateBlockEntity;
import com.stargate.mod.stargate.block.StargateDhdBlock;
import com.stargate.mod.stargate.dhd.DhdLayout;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the DHD's 38 keys and its dome on top of the static body, so each one can light up on its own: a key
 * glows (emissive texture, full brightness) once its symbol is dialed on the linked gate (even while the ring
 * is still bringing it up), and the dome glows
 * once the point of origin is in or while the gate holds an outgoing wormhole. An incoming wormhole does not
 * light the receiving DHD.
 *
 * <p>Geometry comes from {@link DhdLayout}; textures from {@code textures/block/dhd_keys(_lit).png}
 * (8 × 6 cells: glyphs 1–38 on transparent, then key top, dome, key sides). Each glyph is a separate quad
 * laid on its key and kept upright for the operator, whatever the key's angle, so it reads the same as in chat.
 *
 * <p><i>FR —</i> Dessine les 38 touches du DHD et son dôme par-dessus le corps fixe, pour que chacun puisse
 * s'allumer séparément : une touche brille (texture lumineuse, pleine luminosité) dès que son symbole est
 * composé sur la porte reliée (même pendant que l'anneau l'amène encore), et le dôme brille
 * dès que le point d'origine est entré ou tant que la porte tient
 * un trou de ver sortant. Un trou de ver entrant n'allume pas le DHD de la porte appelée.
 *
 * <p>La géométrie vient de {@link DhdLayout} ; les textures de {@code textures/block/dhd_keys(_lit).png}
 * (8 × 6 cases : glyphes 1 à 38 sur fond transparent, puis dessus des touches, dôme, flancs). Chaque glyphe est
 * un quad posé sur sa touche et gardé droit pour l'opérateur, quel que soit l'angle de la touche, pour se lire
 * comme dans le chat.
 */
public class DhdRenderer implements BlockEntityRenderer<DhdBlockEntity, DhdRenderer.State> {
    private static final Identifier KEYS = Identifier.fromNamespaceAndPath(StargateMod.MODID, "textures/block/dhd_keys.png");
    private static final Identifier KEYS_LIT = Identifier.fromNamespaceAndPath(StargateMod.MODID, "textures/block/dhd_keys_lit.png");
    private static final int COLS = 8, ROWS = 6, TOP_CELL = 38, DOME_CELL = 39, SIDE_CELL = 40;
    /** Glyph lift above the key top, against z-fighting. / <i>FR —</i> Glyphe surélevé au-dessus de la touche, contre le z-fighting. */
    private static final double GLYPH_LIFT = 0.0015;
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Angular slices per key, so keys follow the ring's curve. / <i>FR —</i> Tranches angulaires par touche, pour suivre la courbe de l'anneau. */
    private static final int SLICES = 3;

    /**
     * What the renderer needs, captured once per frame.
     *
     * <p><i>FR —</i> Ce dont le rendu a besoin, capturé une fois par image.
     */
    public static class State extends BlockEntityRenderState {
        /** Facing step X / Z. / <i>FR —</i> Pas X / Z de l'orientation. */
        public int fx, fz = 1;
        /** Lit symbols, indexed by symbol id. / <i>FR —</i> Symboles allumés, indexés par identifiant. */
        public final boolean[] lit = new boolean[StargateSymbol.COUNT];
        /** Whether the dome glows. / <i>FR —</i> Indique si le dôme brille. */
        public boolean domeLit;

        /** Creates an empty state. / <i>FR —</i> Crée un état vide. */
        public State() {
        }
    }

    /**
     * Creates the renderer; called by the game.
     *
     * <p><i>FR —</i> Crée le rendu ; appelé par le jeu.
     *
     * @param context renderer context / contexte du rendu
     */
    public DhdRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DhdBlockEntity dhd, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(dhd, state, partialTicks, cameraPosition, breakProgress);
        Direction facing = dhd.getBlockState().hasProperty(StargateDhdBlock.FACING)
                ? dhd.getBlockState().getValue(StargateDhdBlock.FACING) : Direction.SOUTH;
        state.fx = facing.getStepX();
        state.fz = facing.getStepZ();
        java.util.Arrays.fill(state.lit, false);
        state.domeLit = false;
        if (dhd.getLevel() == null) {
            return;
        }
        dhd.getLinkedGate()
                .map(pos -> dhd.getLevel().getBlockEntity(pos))
                .filter(StargateBlockEntity.class::isInstance)
                .map(StargateBlockEntity.class::cast)
                .filter(gate -> !gate.isIncoming())
                .ifPresent(gate -> {
                    for (StargateSymbol s : gate.getDialed()) {
                        state.lit[s.id()] = true;
                    }
                    state.domeLit = state.lit[0] || gate.getGateState().isConnected();
                });
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(KEYS),
                (pose, buffer) -> drawAll(pose, buffer, state, false, state.lightCoords));
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(KEYS_LIT),
                (pose, buffer) -> drawAll(pose, buffer, state, true, FULL_BRIGHT));
    }

    /** Draws either the unlit or the lit keys (and dome). / <i>FR —</i> Dessine soit les touches éteintes, soit les allumées (et le dôme). */
    private static void drawAll(PoseStack.Pose pose, VertexConsumer buffer, State s, boolean litPass, int light) {
        for (boolean outer : new boolean[]{true, false}) {
            double r0 = outer ? DhdLayout.OUTER_R0 : DhdLayout.INNER_R0;
            double r1 = outer ? DhdLayout.OUTER_R1 : DhdLayout.INNER_R1;
            for (int i = 0; i < DhdLayout.KEYS_PER_RING; i++) {
                int symbol = DhdLayout.symbolAt(outer, i);
                if (s.lit[symbol] == litPass) {
                    double[] ang = DhdLayout.keyAngles(i);
                    key(pose, buffer, s, r0, r1, ang, light);
                    glyph(pose, buffer, s, DhdLayout.glyphCircle(outer), (ang[0] + ang[1]) / 2, symbol - 1, light);
                }
            }
        }
        if (s.domeLit == litPass) {
            dome(pose, buffer, s, light);
        }
    }

    private static void key(PoseStack.Pose pose, VertexConsumer b, State s, double r0, double r1, double[] ang, int light) {
        double h = DhdLayout.KEY_HEIGHT;
        double start = ang[0], end = ang[1];
        for (int k = 0; k < SLICES; k++) {
            // u runs left to right as seen by the operator, i.e. decreasing angle. / u va de gauche à droite pour l'opérateur, angle décroissant.
            double a0 = end - (end - start) * k / SLICES, a1 = end - (end - start) * (k + 1) / SLICES;
            float u0 = (float) k / SLICES, u1 = (float) (k + 1) / SLICES;
            quad(pose, b, s, light, TOP_CELL,
                    DhdLayout.onConsole(r1, a0, h), u0, 0f,
                    DhdLayout.onConsole(r0, a0, h), u0, 1f,
                    DhdLayout.onConsole(r0, a1, h), u1, 1f,
                    DhdLayout.onConsole(r1, a1, h), u1, 0f);
            // outer and inner walls / parois extérieure et intérieure
            quad(pose, b, s, light, SIDE_CELL,
                    DhdLayout.onConsole(r1, a0, h), 0, 0, DhdLayout.onConsole(r1, a1, h), 1, 0,
                    DhdLayout.onConsole(r1, a1, 0), 1, 1, DhdLayout.onConsole(r1, a0, 0), 0, 1);
            quad(pose, b, s, light, SIDE_CELL,
                    DhdLayout.onConsole(r0, a1, h), 0, 0, DhdLayout.onConsole(r0, a0, h), 1, 0,
                    DhdLayout.onConsole(r0, a0, 0), 1, 1, DhdLayout.onConsole(r0, a1, 0), 0, 1);
        }
        // radial walls / parois radiales
        for (double a : new double[]{start, end}) {
            quad(pose, b, s, light, SIDE_CELL,
                    DhdLayout.onConsole(r0, a, h), 0, 0, DhdLayout.onConsole(r1, a, h), 1, 0,
                    DhdLayout.onConsole(r1, a, 0), 1, 1, DhdLayout.onConsole(r0, a, 0), 0, 1);
        }
    }

    /**
     * The glyph: a square on the key top, sides along the console's e1 (right) and e2 (up the slope), so it is
     * upright for the operator. Transparent around the glyph, so only the symbol shows.
     *
     * <p><i>FR —</i> Le glyphe : un carré sur le dessus de la touche, côtés selon e1 (droite) et e2 (haut de la
     * pente), donc droit pour l'opérateur. Transparent autour du glyphe : seul le symbole apparaît.
     */
    private static void glyph(PoseStack.Pose pose, VertexConsumer b, State s, double[] circle, double angle, int cell, int light) {
        double[] c = DhdLayout.onConsole(circle[0], angle, DhdLayout.KEY_HEIGHT + GLYPH_LIFT);
        double h = circle[1];
        quad(pose, b, s, light, cell,
                corner(c, -h, h), 0f, 0f,
                corner(c, -h, -h), 0f, 1f,
                corner(c, h, -h), 1f, 1f,
                corner(c, h, h), 1f, 0f);
    }

    /** {@code c + right·e1 + up·e2}. / <i>FR —</i> {@code c + droite·e1 + haut·e2}. */
    private static double[] corner(double[] c, double right, double up) {
        return new double[]{c[0] + right + up * DhdLayout.E2X, c[1] + up * DhdLayout.E2Y, c[2] + up * DhdLayout.E2Z};
    }

    /** Low-poly hemisphere on the console centre. / <i>FR —</i> Hémisphère simplifiée au centre de la console. */
    private static void dome(PoseStack.Pose pose, VertexConsumer b, State s, int light) {
        int lat = 5, lon = 16;
        double rad = DhdLayout.DOME_RADIUS;
        for (int i = 0; i < lat; i++) {
            double t0 = Math.PI / 2 * i / lat, t1 = Math.PI / 2 * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
                float u0 = (float) j / lon, u1 = (float) (j + 1) / lon, v0 = (float) i / lat, v1 = (float) (i + 1) / lat;
                quad(pose, b, s, light, DOME_CELL,
                        DhdLayout.onConsole(rad * Math.cos(t0), p0, rad * Math.sin(t0)), u0, v0,
                        DhdLayout.onConsole(rad * Math.cos(t0), p1, rad * Math.sin(t0)), u1, v0,
                        DhdLayout.onConsole(rad * Math.cos(t1), p1, rad * Math.sin(t1)), u1, v1,
                        DhdLayout.onConsole(rad * Math.cos(t1), p0, rad * Math.sin(t1)), u0, v1);
            }
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer b, State s, int light, int cell,
                             double[] p0, float u0, float v0, double[] p1, float u1, float v1,
                             double[] p2, float u2, float v2, double[] p3, float u3, float v3) {
        double[] n = DhdLayout.toWorld(new double[]{0.5 + DhdLayout.NX, DhdLayout.NY, 0.5 + DhdLayout.NZ}, s.fx, s.fz);
        float nx = (float) (n[0] - 0.5), ny = (float) n[1], nz = (float) (n[2] - 0.5);
        vertex(pose, b, s, light, cell, p0, u0, v0, nx, ny, nz);
        vertex(pose, b, s, light, cell, p1, u1, v1, nx, ny, nz);
        vertex(pose, b, s, light, cell, p2, u2, v2, nx, ny, nz);
        vertex(pose, b, s, light, cell, p3, u3, v3, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer b, State s, int light, int cell,
                               double[] model, float u, float v, float nx, float ny, float nz) {
        double[] w = DhdLayout.toWorld(model, s.fx, s.fz);
        // Inset the UVs a little so neighbouring cells never bleed in. / UV légèrement rentrés pour ne pas déborder sur les cases voisines.
        float cu = (cell % COLS + 0.03f + 0.94f * u) / COLS;
        float cv = (cell / COLS + 0.03f + 0.94f * v) / ROWS;
        b.addVertex(pose, (float) w[0], (float) w[1], (float) w[2])
                .setColor(0xFFFFFFFF)
                .setUv(cu, cv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
