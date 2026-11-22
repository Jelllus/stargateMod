package com.stargate.mod.stargate.transport;

/**
 * Local frame of a gate's event horizon, and the transfer of positions, motion and facing from one gate to
 * another. Pure math (no Minecraft types) so it can be tested.
 *
 * <p>Local coordinates: {@code u} to the right (as seen by someone standing in front, facing the gate),
 * {@code v} up, {@code w} along the front normal (positive in front). The origin is the centre of the
 * horizon, 2.5 blocks above the control block's base, in the middle of the ring's thickness.
 *
 * <ul>
 *     <li>Ring along X: right = +X, front = +Z.</li>
 *     <li>Ring along Z: right = +Z, front = −X (the X layout turned by −90° around Y, like the gate model).</li>
 * </ul>
 *
 * <p>Going through a gate is a half turn: the traveller walks <em>into</em> the source front (−w) and comes
 * <em>out of</em> the target front (+w), keeping their own left and right.
 *
 * <p><i>FR —</i> Repère local de l'horizon des événements d'une porte, et transfert des positions, du
 * mouvement et de l'orientation d'une porte à l'autre. Calcul pur (sans type Minecraft), donc testable.
 *
 * <p>Coordonnées locales : {@code u} vers la droite (vu par quelqu'un debout devant, face à la porte),
 * {@code v} vers le haut, {@code w} le long de la normale avant (positif devant). L'origine est le centre de
 * l'horizon, 2,5 blocs au-dessus de la base du bloc de contrôle, au milieu de l'épaisseur de l'anneau.
 *
 * <ul>
 *     <li>Anneau selon X : droite = +X, avant = +Z.</li>
 *     <li>Anneau selon Z : droite = +Z, avant = −X (la disposition X tournée de −90° autour de Y, comme le
 *     modèle de la porte).</li>
 * </ul>
 *
 * <p>Traverser une porte est un demi-tour : le voyageur entre <em>dans</em> l'avant de la porte source (−w)
 * et ressort <em>par</em> l'avant de la porte cible (+w), en gardant sa gauche et sa droite.
 *
 * @param cx centre X / centre X
 * @param cy centre Y / centre Y
 * @param cz centre Z / centre Z
 * @param rx right vector X / vecteur droite X
 * @param rz right vector Z / vecteur droite Z
 * @param nx front normal X / normale avant X
 * @param nz front normal Z / normale avant Z
 */
public record GateBasis(double cx, double cy, double cz, double rx, double rz, double nx, double nz) {
    /**
     * Height of the horizon centre above the control block's base.
     * / <i>FR —</i> Hauteur du centre de l'horizon au-dessus de la base du bloc de contrôle.
     */
    public static final double CENTRE_HEIGHT = 2.5;

    /**
     * Builds the frame of a gate.
     *
     * <p><i>FR —</i> Construit le repère d'une porte.
     *
     * @param controlX control block X / X du bloc de contrôle
     * @param controlY control block Y / Y du bloc de contrôle
     * @param controlZ control block Z / Z du bloc de contrôle
     * @param alongX   {@code true} if the ring runs along X, {@code false} along Z / {@code true} si l'anneau
     *                 est orienté selon X, {@code false} selon Z
     * @return the frame / le repère
     */
    public static GateBasis of(int controlX, int controlY, int controlZ, boolean alongX) {
        double cx = controlX + 0.5, cy = controlY + CENTRE_HEIGHT, cz = controlZ + 0.5;
        return alongX ? new GateBasis(cx, cy, cz, 1, 0, 0, 1) : new GateBasis(cx, cy, cz, 0, 1, -1, 0);
    }

    /**
     * World position → local {u, v, w}.
     *
     * <p><i>FR —</i> Position monde → {u, v, w} local.
     *
     * @param x world X / X monde
     * @param y world Y / Y monde
     * @param z world Z / Z monde
     * @return {u, v, w}
     */
    public double[] toLocal(double x, double y, double z) {
        double dx = x - cx, dz = z - cz;
        return new double[]{dx * rx + dz * rz, y - cy, dx * nx + dz * nz};
    }

    /**
     * Local {u, v, w} → world position.
     *
     * <p><i>FR —</i> {u, v, w} local → position monde.
     *
     * @param u right / droite
     * @param v up / haut
     * @param w front / avant
     * @return {x, y, z}
     */
    public double[] toWorld(double u, double v, double w) {
        return new double[]{cx + u * rx + w * nx, cy + v, cz + u * rz + w * nz};
    }

    /**
     * World direction → local {u, v, w} (no translation).
     *
     * <p><i>FR —</i> Direction monde → {u, v, w} local (sans translation).
     *
     * @param dx X component / composante X
     * @param dy Y component / composante Y
     * @param dz Z component / composante Z
     * @return {u, v, w}
     */
    public double[] dirToLocal(double dx, double dy, double dz) {
        return new double[]{dx * rx + dz * rz, dy, dx * nx + dz * nz};
    }

    /**
     * Local direction → world {x, y, z} (no translation).
     *
     * <p><i>FR —</i> Direction locale → {x, y, z} monde (sans translation).
     *
     * @param u right / droite
     * @param v up / haut
     * @param w front / avant
     * @return {x, y, z}
     */
    public double[] dirToWorld(double u, double v, double w) {
        return new double[]{u * rx + w * nx, v, u * rz + w * nz};
    }

    /**
     * Minecraft yaw (degrees) of a horizontal direction: 0 = south (+Z), 90 = west (−X).
     *
     * <p><i>FR —</i> Lacet Minecraft (degrés) d'une direction horizontale : 0 = sud (+Z), 90 = ouest (−X).
     *
     * @param dx X component / composante X
     * @param dz Z component / composante Z
     * @return degrees / degrés
     */
    public static float yawOf(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /**
     * Where something ends up after going through {@code from} into {@code to}.
     *
     * <p><i>FR —</i> Où arrive ce qui traverse {@code from} vers {@code to}.
     *
     * @param from     source gate / porte source
     * @param to       target gate / porte cible
     * @param pos      world position {x, y, z} at the source / position monde à la source
     * @param motion   world motion {x, y, z} / mouvement monde
     * @param yaw      facing, degrees / orientation, degrés
     * @param exitPush how far in front of the target to place the traveller, in blocks / distance devant la
     *                 cible où placer le voyageur, en blocs
     * @return the exit / la sortie
     */
    public static Exit transfer(GateBasis from, GateBasis to, double[] pos, double[] motion, float yaw, double exitPush) {
        double[] p = from.toLocal(pos[0], pos[1], pos[2]);
        double[] m = from.dirToLocal(motion[0], motion[1], motion[2]);
        // Half turn around the vertical: u and w flip. / Demi-tour autour de la verticale : u et w s'inversent.
        double exitW = Math.max(-p[2], 0) + exitPush;
        double[] worldPos = to.toWorld(-p[0], p[1], exitW);
        double[] worldMotion = to.dirToWorld(-m[0], m[1], -m[2]);
        float turn = yawOf(to.nx, to.nz) - yawOf(-from.nx, -from.nz);
        return new Exit(worldPos, worldMotion, wrapDegrees(yaw + turn));
    }

    private static float wrapDegrees(float deg) {
        float d = deg % 360f;
        if (d >= 180f) {
            d -= 360f;
        } else if (d < -180f) {
            d += 360f;
        }
        return d;
    }

    /**
     * Result of {@link #transfer}.
     *
     * <p><i>FR —</i> Résultat de {@link #transfer}.
     *
     * @param pos    world position {x, y, z} / position monde
     * @param motion world motion {x, y, z} / mouvement monde
     * @param yaw    facing, degrees / orientation, degrés
     */
    public record Exit(double[] pos, double[] motion, float yaw) {
    }
}
