package com.stargate.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.stargate.mod.StargateMod;
import com.stargate.mod.stargate.address.StargateSymbol;
import com.stargate.mod.stargate.block.StargateBlockEntity;
import com.stargate.mod.stargate.connection.EventHorizonShape;
import com.stargate.mod.stargate.connection.WormholeState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the moving parts of a formed gate, every frame: the inner symbol track, turning as the gate dials,
 * and the event horizon.
 *
 * <h2>Symbol track</h2>
 * The ring that carries the 39 symbols (the mod's own glyphs, {@code textures/block/gate_track.png}) is cut out
 * of the static gate model and drawn here, rotated so that the symbol given by
 * {@link StargateBlockEntity#getRingPosition} sits under the top chevron. Symbols are read upright at the
 * top, from the front and from the back.
 *
 * <h2>Event horizon</h2>
 * The kawoosh while the vortex forms, the rippling
 * surface while open, and the collapse while closing. The shape comes from {@link EventHorizonShape}; this
 * class only turns it into a polar mesh (rings × segments) in the plane of the ring.
 *
 * <p>The surface is drawn emissive and translucent with the {@code event_horizon} texture, whose oval centre
 * is sampled with a slow rotation for the swirl.
 *
 * <p><i>FR —</i> Dessine les parties mobiles d'une porte formée, à chaque image : la piste intérieure des
 * symboles, qui tourne pendant la numérotation, et l'horizon des événements.
 *
 * <p>La piste des 39 symboles (les glyphes du mod, {@code textures/block/gate_track.png}) est retirée du
 * modèle fixe de la porte et dessinée ici, tournée pour que le symbole donné par
 * {@link StargateBlockEntity#getRingPosition} soit sous le chevron du haut. Les symboles se lisent droits en
 * haut, de face comme de dos.
 *
 * <p>Horizon des événements : le kawoosh pendant la
 * formation du vortex, la surface ondulante pendant l'ouverture, et l'effondrement à la fermeture. La forme
 * vient de {@link EventHorizonShape} ; cette classe la transforme seulement en maillage polaire
 * (anneaux × segments) dans le plan de l'anneau.
 *
 * <p>La surface est dessinée lumineuse et translucide avec la texture {@code event_horizon}, dont l'ovale
 * central est échantillonné avec une rotation lente pour le tourbillon.
 */
public class StargateRenderer implements BlockEntityRenderer<StargateBlockEntity, StargateRenderer.State> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(StargateMod.MODID, "textures/block/event_horizon.png");
    private static final Identifier TRACK =
            Identifier.fromNamespaceAndPath(StargateMod.MODID, "textures/block/gate_track.png");

    /**
     * Symbol track, ring-centred block units (from {@code tools/glb2obj.py}): radii and face depth.
     * <i>FR —</i> Piste des symboles, en blocs depuis le centre de l'anneau : rayons et profondeur des faces.
     */
    private static final float TRACK_R0 = 2.0225f, TRACK_R1 = 2.1645f, TRACK_Z = 0.1083f;
    private static final int TRACK_COLS = 8, TRACK_ROWS = 5, TRACK_SLICES = 4;
    private static final double SYMBOL_STEP = 2 * Math.PI / StargateSymbol.COUNT;

    /** Mesh resolution. / <i>FR —</i> Résolution du maillage. */
    private static final int RINGS = 18;
    private static final int SEGMENTS = 56;

    /** Centre and half-size of the blue oval in the texture (UV). / <i>FR —</i> Centre et demi-taille de l'ovale bleu dans la texture (UV). */
    private static final float TEX_CU = 0.495f, TEX_CV = 0.515f, TEX_RU = 0.22f, TEX_RV = 0.35f;

    private static final int FULL_BRIGHT = 0xF000F0;

    /**
     * What the renderer needs from the block entity, captured once per frame.
     *
     * <p><i>FR —</i> Ce dont le rendu a besoin du block entity, capturé une fois par image.
     */
    public static class State extends BlockEntityRenderState {
        /** Whether the ring is complete. / <i>FR —</i> Indique si l'anneau est complet. */
        public boolean formed;
        /** Axis of the ring. / <i>FR —</i> Axe de l'anneau. */
        public Direction.Axis axis = Direction.Axis.X;
        /** Gate state. / <i>FR —</i> État de la porte. */
        public WormholeState gateState = WormholeState.IDLE;
        /** Ticks in that state. / <i>FR —</i> Ticks dans cet état. */
        public float age;
        /** Continuous time, for the swirl. / <i>FR —</i> Temps continu, pour le tourbillon. */
        public float time;
        /** Phase lengths. / <i>FR —</i> Durées des phases. */
        public int engageTicks, closeTicks;
        /** Symbol under the top chevron. / <i>FR —</i> Symbole sous le chevron du haut. */
        public double ringPosition;

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
    public StargateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StargateBlockEntity gate, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(gate, state, partialTicks, cameraPosition, breakProgress);
        state.formed = gate.isFormed();
        state.axis = gate.getAxis().orElse(Direction.Axis.X);
        state.gateState = gate.getGateState();
        state.age = gate.getPhaseAge(partialTicks);
        state.time = gate.getLevel() == null ? 0f : (gate.getLevel().getGameTime() % 1_000_000L) + partialTicks;
        state.engageTicks = gate.getEngageTicks();
        state.closeTicks = gate.getCloseTicks();
        state.ringPosition = gate.getRingPosition(partialTicks);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5f, 2.5f, 0.5f);
        if (state.axis == Direction.Axis.Z) {
            poseStack.rotate(Axis.YP.rotationDegrees(-90f));
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TRACK),
                (pose, buffer) -> drawTrack(pose, buffer, state));
        poseStack.popPose();
        if (!EventHorizonShape.isVisible(state.gateState)) {
            return;
        }
        float scale = EventHorizonShape.radiusScale(state.gateState, state.age, state.engageTicks, state.closeTicks);
        float alpha = EventHorizonShape.alpha(state.gateState, state.age, state.closeTicks);
        if (scale <= 0.001f || alpha <= 0.001f) {
            return;
        }
        poseStack.pushPose();
        // Ring centre, then turn the X-axis layout to face the ring's axis (same rotation as the gate model).
        // Centre de l'anneau, puis rotation de la disposition « axe X » vers l'axe de l'anneau (même rotation que le modèle).
        poseStack.translate(0.5f, 2.5f, 0.5f);
        if (state.axis == Direction.Axis.Z) {
            poseStack.rotate(Axis.YP.rotationDegrees(-90f));
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(TEXTURE),
                (pose, buffer) -> drawSurface(pose, buffer, state, scale, alpha));
        poseStack.popPose();
    }

    /**
     * The 39 symbol cells around the ring, front and back. Angles run clockwise from the top as seen from the
     * front (+Z); symbol {@code k} is centred at {@code (k - position) · step}.
     *
     * <p><i>FR —</i> Les 39 cases de symboles autour de l'anneau, devant et derrière. Les angles tournent dans
     * le sens horaire depuis le haut vu de face (+Z) ; le symbole {@code k} est centré en
     * {@code (k - position) · pas}.
     */
    private static void drawTrack(PoseStack.Pose pose, VertexConsumer buffer, State s) {
        for (int k = 0; k < StargateSymbol.COUNT; k++) {
            double centre = (k - s.ringPosition) * SYMBOL_STEP;
            for (int i = 0; i < TRACK_SLICES; i++) {
                double a0 = centre + SYMBOL_STEP * ((double) i / TRACK_SLICES - 0.5);
                double a1 = centre + SYMBOL_STEP * ((double) (i + 1) / TRACK_SLICES - 0.5);
                float u0 = (float) i / TRACK_SLICES, u1 = (float) (i + 1) / TRACK_SLICES;
                // front: u grows clockwise, outer edge at the top of the glyph / devant : u croît en sens horaire, bord extérieur en haut du glyphe
                trackVertex(pose, buffer, s, k, a0, TRACK_R1, TRACK_Z, u0, 0f, 1f);
                trackVertex(pose, buffer, s, k, a0, TRACK_R0, TRACK_Z, u0, 1f, 1f);
                trackVertex(pose, buffer, s, k, a1, TRACK_R0, TRACK_Z, u1, 1f, 1f);
                trackVertex(pose, buffer, s, k, a1, TRACK_R1, TRACK_Z, u1, 0f, 1f);
                // back: mirrored so the glyph reads the right way from behind / derrière : en miroir pour se lire à l'endroit
                trackVertex(pose, buffer, s, k, a1, TRACK_R1, -TRACK_Z, 1f - u1, 0f, -1f);
                trackVertex(pose, buffer, s, k, a1, TRACK_R0, -TRACK_Z, 1f - u1, 1f, -1f);
                trackVertex(pose, buffer, s, k, a0, TRACK_R0, -TRACK_Z, 1f - u0, 1f, -1f);
                trackVertex(pose, buffer, s, k, a0, TRACK_R1, -TRACK_Z, 1f - u0, 0f, -1f);
            }
        }
    }

    private static void trackVertex(PoseStack.Pose pose, VertexConsumer buffer, State s, int cell, double angle,
                                    float r, float z, float u, float v, float nz) {
        // Inset the UVs so neighbouring cells never bleed in. / UV rentrés pour ne pas déborder sur les cases voisines.
        float cu = (cell % TRACK_COLS + 0.005f + 0.99f * u) / TRACK_COLS;
        float cv = (cell / TRACK_COLS + 0.01f + 0.98f * v) / TRACK_ROWS;
        buffer.addVertex(pose, (float) (r * Math.sin(angle)), (float) (r * Math.cos(angle)), z)
                .setColor(0xFFFFFFFF)
                .setUv(cu, cv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(s.lightCoords)
                .setNormal(pose, 0f, 0f, nz);
    }

    private static void drawSurface(PoseStack.Pose pose, VertexConsumer buffer, State s, float scale, float alpha) {
        float radius = EventHorizonShape.RADIUS * scale;
        float swirl = EventHorizonShape.swirl(s.time);
        float jet = s.gateState == WormholeState.ENGAGING
                ? EventHorizonShape.kawooshLength(s.age / s.engageTicks) / EventHorizonShape.KAWOOSH_LENGTH : 0f;

        // Precompute the grid. / Pré-calcul de la grille.
        float[][] x = new float[RINGS + 1][SEGMENTS + 1], y = new float[RINGS + 1][SEGMENTS + 1], z = new float[RINGS + 1][SEGMENTS + 1];
        float[][] u = new float[RINGS + 1][SEGMENTS + 1], v = new float[RINGS + 1][SEGMENTS + 1];
        for (int i = 0; i <= RINGS; i++) {
            float rn = (float) i / RINGS;
            for (int j = 0; j <= SEGMENTS; j++) {
                float theta = (float) (2 * Math.PI * j / SEGMENTS);
                float cos = (float) Math.cos(theta), sin = (float) Math.sin(theta);
                x[i][j] = rn * radius * cos;
                y[i][j] = rn * radius * sin;
                z[i][j] = EventHorizonShape.offset(s.gateState, s.age, s.engageTicks, s.closeTicks, rn, theta);
                float t = theta + swirl;
                u[i][j] = TEX_CU + TEX_RU * rn * (float) Math.cos(t);
                v[i][j] = TEX_CV + TEX_RV * rn * (float) Math.sin(t);
            }
        }
        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < SEGMENTS; j++) {
                vertex(pose, buffer, x, y, z, u, v, i, j, alpha, jet);
                vertex(pose, buffer, x, y, z, u, v, i + 1, j, alpha, jet);
                vertex(pose, buffer, x, y, z, u, v, i + 1, j + 1, alpha, jet);
                vertex(pose, buffer, x, y, z, u, v, i, j + 1, alpha, jet);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float[][] x, float[][] y, float[][] z,
                               float[][] u, float[][] v, int i, int j, float alpha, float jet) {
        // The kawoosh glows whiter towards its tip. / Le kawoosh blanchit vers sa pointe.
        float tip = jet * Math.max(0f, 1f - (float) i / RINGS);
        int r = (int) (205 + 50 * tip), g = (int) (225 + 30 * tip);
        int color = ((int) (alpha * 255) << 24) | (r << 16) | (g << 8) | 255;
        buffer.addVertex(pose, x[i][j], y[i][j], z[i][j])
                .setColor(color)
                .setUv(u[i][j], v[i][j])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0f, 0f, 1f);
    }

    /**
     * Renders even when the control block is off-screen, since the gate spans 5×5 blocks and the kawoosh
     * reaches forward.
     *
     * <p><i>FR —</i> Dessine même quand le bloc de contrôle est hors écran, car la porte couvre 5×5 blocs et le
     * kawoosh s'étend vers l'avant.
     */
    @Override
    public AABB getRenderBoundingBox(StargateBlockEntity gate) {
        BlockPos p = gate.getBlockPos();
        float reach = EventHorizonShape.KAWOOSH_LENGTH + 1f;
        return new AABB(p.getX() - reach, p.getY(), p.getZ() - reach, p.getX() + 1 + reach, p.getY() + 6, p.getZ() + 1 + reach);
    }
}
