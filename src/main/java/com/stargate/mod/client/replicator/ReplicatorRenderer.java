package com.stargate.mod.client.replicator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.stargate.mod.StargateMod;
import com.stargate.mod.replicator.ReplicatorEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Draws the replicator from its OBJ mesh ({@code models/entity/replicator.obj}, converted from the SG-1
 * Replicator model) instead of a cube model. The vanilla {@link MobRenderer} still handles everything around
 * it — body rotation, death flip, hurt flash, shadow, name tag — and the mesh is drawn by a layer, with the
 * four legs animated around their hip and knee: a diagonal gait (front-right with back-left, then the other
 * pair), each leg lifting while it swings forward.
 *
 * <p><i>FR —</i> Dessine le réplicateur depuis son maillage OBJ ({@code models/entity/replicator.obj},
 * converti du modèle Replicator SG-1) au lieu d'un modèle en cubes. Le {@link MobRenderer} vanilla gère
 * toujours le reste — rotation du corps, renversement à la mort, flash de dégât, ombre, nom — et le maillage
 * est dessiné par une couche, avec les quatre pattes animées autour de la hanche et du genou : une démarche
 * en diagonale (avant-droite avec arrière-gauche, puis l'autre paire), chaque patte se levant pendant
 * qu'elle avance.
 */
public class ReplicatorRenderer extends MobRenderer<ReplicatorEntity, LivingEntityRenderState, ReplicatorRenderer.EmptyModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(StargateMod.MODID, "textures/entity/replicator.png");
    private static final Identifier MESH =
            Identifier.fromNamespaceAndPath(StargateMod.MODID, "models/entity/replicator.obj");

    /** Mesh units (1 = 1 m in the source model) to blocks. / <i>FR —</i> Unités du maillage (1 = 1 m dans le modèle source) vers blocs. */
    private static final float MODEL_SCALE = 1.5F;

    /**
     * Creates the renderer; called by the game on each resource reload.
     *
     * <p><i>FR —</i> Crée le rendu ; appelé par le jeu à chaque rechargement des ressources.
     *
     * @param context renderer context / contexte du rendu
     */
    public ReplicatorRenderer(EntityRendererProvider.Context context) {
        super(context, new EmptyModel(), 0.45F);
        this.addLayer(new MeshLayer(this, ObjMesh.load(context.getResourceManager(), MESH)));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    /** The cube model is empty: skip it, the layer draws the mesh. / <i>FR —</i> Le modèle en cubes est vide : on le saute, la couche dessine le maillage. */
    @Override
    protected @Nullable RenderType getRenderType(LivingEntityRenderState state, boolean isBodyVisible,
                                                 boolean forceTransparent, boolean appearGlowing) {
        return null;
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    /**
     * Placeholder model with no cubes, required by {@link LivingEntityRenderer}.
     *
     * <p><i>FR —</i> Modèle vide sans cubes, exigé par {@link LivingEntityRenderer}.
     */
    public static class EmptyModel extends EntityModel<LivingEntityRenderState> {
        /** Creates the empty model. / <i>FR —</i> Crée le modèle vide. */
        public EmptyModel() {
            super(new ModelPart(List.of(), Map.of()));
        }
    }

    /**
     * One leg: its two mesh parts, hip and knee pivots, lift axis and gait phase.
     *
     * <p><i>FR —</i> Une patte : ses deux parties de maillage, pivots de hanche et de genou, axe de levée et
     * phase de la démarche.
     */
    private record Leg(ObjMesh.Part upper, ObjMesh.Part lower, float hipX, float hipY, float hipZ,
                       float kneeX, float kneeY, float kneeZ, float liftAxisX, float liftAxisZ,
                       float side, float phase, float twitch) {
        static Leg of(ObjMesh mesh, String name, float hipX, float hipY, float hipZ,
                      float kneeX, float kneeY, float kneeZ, float phase, float twitch) {
            // Horizontal leg direction d; lifting rotates around d × up = (-dz, 0, dx), which raises the tip.
            // Direction horizontale d de la patte ; lever = tourner autour de d × haut = (-dz, 0, dx), qui lève la pointe.
            float dx = kneeX - hipX, dz = kneeZ - hipZ;
            float len = Mth.sqrt(dx * dx + dz * dz);
            return new Leg(mesh.part(name + "_upper"), mesh.part(name + "_lower"), hipX, hipY, hipZ,
                    kneeX, kneeY, kneeZ, -dz / len, dx / len, Math.signum(hipX), phase, twitch);
        }
    }

    /**
     * Draws the mesh with the animated legs.
     *
     * <p><i>FR —</i> Dessine le maillage avec les pattes animées.
     */
    private static class MeshLayer extends RenderLayer<LivingEntityRenderState, EmptyModel> {
        private final ObjMesh.Part body;
        private final Leg[] legs;
        private final Quaternionf rotation = new Quaternionf();

        MeshLayer(RenderLayerParent<LivingEntityRenderState, EmptyModel> parent, ObjMesh mesh) {
            super(parent);
            this.body = mesh.part("body");
            // Pivots measured on the source model (mesh units). / Pivots mesurés sur le modèle source (unités du maillage).
            float pi = Mth.PI;
            this.legs = new Leg[]{
                    Leg.of(mesh, "back_left", 0.0271F, 0.1514F, -0.1007F, 0.2689F, 0.0967F, -0.1655F, 0F, 0F),
                    Leg.of(mesh, "front_right", -0.0136F, 0.1514F, 0.0184F, -0.0992F, 0.0967F, 0.2536F, 0F, 1.7F),
                    Leg.of(mesh, "back_right", -0.0271F, 0.1514F, -0.1007F, -0.2689F, 0.0967F, -0.1655F, pi, 3.1F),
                    Leg.of(mesh, "front_left", 0.0136F, 0.1514F, 0.0184F, 0.0992F, 0.0967F, 0.2536F, pi, 4.6F),
            };
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                           LivingEntityRenderState state, float yRot, float xRot) {
            if (state.isInvisible) {
                return;
            }
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            float walk = state.walkAnimationPos * 0.6662F * 2.0F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            float age = state.ageInTicks;

            poseStack.pushPose();
            // Undo the cube-model frame (y down, 1.5 blocks up) back to the entity's feet, y up.
            // Annule le repère des modèles en cubes (y vers le bas, 1,5 bloc plus haut) : retour aux pieds, y vers le haut.
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0F, -1.501F, 0.0F);
            poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
            // The source model's head points to +Z: turn it to face the entity's front.
            // La tête du modèle source pointe vers +Z : on la tourne vers l'avant de l'entité.
            poseStack.rotate(Axis.YP, Mth.PI);
            // Slight bob while walking. / Léger balancement pendant la marche.
            poseStack.translate(0.0F, Math.abs(Mth.sin(walk)) * 0.012F * speed, 0.0F);

            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
                this.body.emit(pose, buffer, -1, lightCoords, overlay);
            });

            for (Leg leg : this.legs) {
                float t = walk + leg.phase();
                // Forward while lifted, back while planted; small idle twitch when standing still.
                // En avant quand levée, en arrière quand posée ; petit frémissement au repos.
                // Head is +Z in mesh space, so "forward" swings toward +Z. / La tête est en +Z dans le maillage : « avant » = vers +Z.
                float forward = Mth.cos(t) * 0.45F * speed;
                float lift = Math.max(0.0F, Mth.sin(t)) * 0.55F * speed
                        + (1.0F - speed) * Math.max(0.0F, Mth.sin(age * 0.08F + leg.twitch())) * 0.06F;

                poseStack.pushPose();
                poseStack.rotateAround(this.rotation.rotationY(forward * leg.side()), leg.hipX(), leg.hipY(), leg.hipZ());
                poseStack.rotateAround(this.rotation.rotationAxis(lift, leg.liftAxisX(), 0.0F, leg.liftAxisZ()),
                        leg.hipX(), leg.hipY(), leg.hipZ());
                collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
                        (pose, buffer) -> leg.upper().emit(pose, buffer, -1, lightCoords, overlay));
                // The shin folds down as the thigh rises, keeping the foot pointing at the ground.
                // Le tibia se replie quand la cuisse se lève, gardant le pied vers le sol.
                poseStack.rotateAround(this.rotation.rotationAxis(-lift * 0.9F, leg.liftAxisX(), 0.0F, leg.liftAxisZ()),
                        leg.kneeX(), leg.kneeY(), leg.kneeZ());
                collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE),
                        (pose, buffer) -> leg.lower().emit(pose, buffer, -1, lightCoords, overlay));
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }
}
