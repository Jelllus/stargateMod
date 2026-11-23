package com.stargate.mod.stargate.dhd;

import com.stargate.mod.stargate.address.StargateSymbol;

/**
 * Geometry of the DHD console, shared by the renderer (where each key is drawn) and the block (which key a
 * click lands on). Pure math, no Minecraft types.
 *
 * <h2>Layout</h2>
 * The console is a disc tilted {@value #TILT_DEGREES}° towards the front of the DHD. On it:
 * <ul>
 *     <li>an outer ring of 19 keys: symbols 1 to 19, clockwise from the top (the back of the console);</li>
 *     <li>an inner ring of 19 keys: symbols 20 to 38, same order;</li>
 *     <li>the dome in the centre: the point of origin.</li>
 * </ul>
 *
 * <h2>Coordinates</h2>
 * <i>Model space</i>: block units, origin at the block's corner, the DHD facing +Z (south). On the console,
 * {@code e1} points right (+X, as seen by the operator) and {@code e2} up the slope (towards the back).
 * {@link #toWorld} turns model space into block space for any facing.
 *
 * <p><i>FR —</i> Géométrie de la console du DHD, partagée par le rendu (où chaque touche est dessinée) et le
 * bloc (sur quelle touche tombe un clic). Calcul pur, sans type Minecraft.
 *
 * <h2>Disposition</h2>
 * La console est un disque incliné de {@value #TILT_DEGREES}° vers l'avant du DHD. Dessus :
 * <ul>
 *     <li>un anneau extérieur de 19 touches : symboles 1 à 19, dans le sens horaire depuis le haut (l'arrière
 *     de la console) ;</li>
 *     <li>un anneau intérieur de 19 touches : symboles 20 à 38, même ordre ;</li>
 *     <li>le dôme au centre : le point d'origine.</li>
 * </ul>
 *
 * <h2>Coordonnées</h2>
 * <i>Espace modèle</i> : unités de bloc, origine au coin du bloc, DHD tourné vers +Z (sud). Sur la console,
 * {@code e1} pointe à droite (+X, vu par l'opérateur) et {@code e2} vers le haut de la pente (vers
 * l'arrière). {@link #toWorld} passe de l'espace modèle à l'espace du bloc pour n'importe quelle orientation.
 */
public final class DhdLayout {
    /** Console tilt. / <i>FR —</i> Inclinaison de la console. */
    public static final double TILT_DEGREES = 25.0;
    /** Centre of the console surface (model space). / <i>FR —</i> Centre de la surface de la console (espace modèle). */
    public static final double CX = 0.5, CY = 0.86, CZ = 0.5;
    /** Console disc radius. / <i>FR —</i> Rayon du disque de la console. */
    public static final double CONSOLE_RADIUS = 0.47;
    /** Dome radius. / <i>FR —</i> Rayon du dôme. */
    public static final double DOME_RADIUS = 0.095;
    /** Inner and outer key rings, radii. / <i>FR —</i> Anneaux de touches intérieur et extérieur, rayons. */
    public static final double INNER_R0 = 0.115, INNER_R1 = 0.30, OUTER_R0 = 0.315, OUTER_R1 = 0.455;
    /** Height of a key above the console. / <i>FR —</i> Hauteur d'une touche au-dessus de la console. */
    public static final double KEY_HEIGHT = 0.018;
    /** Keys per ring. / <i>FR —</i> Touches par anneau. */
    public static final int KEYS_PER_RING = 19;
    /** Angular gap between two keys, radians. / <i>FR —</i> Espace angulaire entre deux touches, en radians. */
    public static final double KEY_GAP = Math.toRadians(1.6);

    private static final double TILT = Math.toRadians(TILT_DEGREES);
    /** Console normal (model space). / <i>FR —</i> Normale de la console (espace modèle). */
    public static final double NX = 0, NY = Math.cos(TILT), NZ = Math.sin(TILT);
    /** Up-the-slope axis e2 (model space); e1 is +X. / <i>FR —</i> Axe e2 vers le haut de la pente ; e1 est +X. */
    public static final double E2X = 0, E2Y = Math.sin(TILT), E2Z = -Math.cos(TILT);

    /** {@link #pick} result for the dome. / <i>FR —</i> Résultat de {@link #pick} pour le dôme. */
    public static final int DOME = StargateSymbol.POINT_OF_ORIGIN.id();
    /** {@link #pick} result when nothing is hit. / <i>FR —</i> Résultat de {@link #pick} quand rien n'est touché. */
    public static final int NONE = -1;

    private DhdLayout() {
    }

    /**
     * Symbol of a key position.
     *
     * <p><i>FR —</i> Symbole d'une position de touche.
     *
     * @param outer {@code true} for the outer ring / {@code true} pour l'anneau extérieur
     * @param index 0 to 18, clockwise from the top / de 0 à 18, sens horaire depuis le haut
     * @return symbol id, 1 to 38 / identifiant du symbole, de 1 à 38
     */
    public static int symbolAt(boolean outer, int index) {
        return (outer ? 1 : 1 + KEYS_PER_RING) + index;
    }

    /**
     * Angular span of key {@code index} on the console, radians from e1 (counter-clockwise): {start, end}.
     * Key 0 is centred on the top; indices go clockwise. Gaps are already removed.
     *
     * <p><i>FR —</i> Étendue angulaire de la touche {@code index} sur la console, en radians depuis e1 (sens
     * trigonométrique) : {début, fin}. La touche 0 est centrée en haut ; les indices tournent dans le sens
     * horaire. Les espaces sont déjà retirés.
     *
     * @param index 0 to 18 / de 0 à 18
     * @return {start, end}, start &lt; end / {début, fin}, début &lt; fin
     */
    public static double[] keyAngles(int index) {
        double step = 2 * Math.PI / KEYS_PER_RING;
        double centre = Math.PI / 2 - index * step;
        return new double[]{centre - step / 2 + KEY_GAP / 2, centre + step / 2 - KEY_GAP / 2};
    }

    /**
     * Where a key's glyph is drawn: the largest circle that fits inside the key (with a small margin), so the
     * glyph can be drawn upright, whatever the key's angle, without spilling over the key's edges.
     *
     * <p><i>FR —</i> Où le glyphe d'une touche est dessiné : le plus grand cercle qui tient dans la touche (avec
     * une petite marge), pour dessiner le glyphe droit, quel que soit l'angle de la touche, sans déborder.
     *
     * @param outer {@code true} for the outer ring / {@code true} pour l'anneau extérieur
     * @return {centre radius on the console, circle radius} / {rayon du centre sur la console, rayon du cercle}
     */
    public static double[] glyphCircle(boolean outer) {
        double r0 = outer ? OUTER_R0 : INNER_R0, r1 = outer ? OUTER_R1 : INNER_R1;
        double sin = Math.sin((2 * Math.PI / KEYS_PER_RING - KEY_GAP) / 2);
        // Touching the outer edge and both radial edges. / Tangent au bord extérieur et aux deux bords radiaux.
        double rho = Math.min(r1 * sin / (1 + sin), (r1 - r0) / 2);
        double centre = r1 - rho;
        return new double[]{centre, rho * 0.92};
    }

    /**
     * A point on the console (model space).
     *
     * <p><i>FR —</i> Un point sur la console (espace modèle).
     *
     * @param r      radius / rayon
     * @param angle  radians from e1 / radians depuis e1
     * @param height above the console surface / au-dessus de la surface de la console
     * @return {x, y, z}
     */
    public static double[] onConsole(double r, double angle, double height) {
        double a = r * Math.cos(angle), b = r * Math.sin(angle);
        return new double[]{CX + a + height * NX, CY + b * E2Y + height * NY, CZ + b * E2Z + height * NZ};
    }

    /**
     * Model space → block space for a DHD facing {@code (fx, fz)}: model +Z becomes the facing direction.
     *
     * <p><i>FR —</i> Espace modèle → espace du bloc pour un DHD tourné vers {@code (fx, fz)} : le +Z du modèle
     * devient la direction d'orientation.
     *
     * @param p  model point {x, y, z} / point modèle
     * @param fx facing X step (-1, 0, 1) / pas X de l'orientation
     * @param fz facing Z step (-1, 0, 1) / pas Z de l'orientation
     * @return block-space point / point dans l'espace du bloc
     */
    public static double[] toWorld(double[] p, int fx, int fz) {
        double dx = p[0] - 0.5, dz = p[2] - 0.5;
        // right r = (fz, -fx), front f = (fx, fz) / droite r = (fz, -fx), avant f = (fx, fz)
        return new double[]{0.5 + dx * fz + dz * fx, p[1], 0.5 - dx * fx + dz * fz};
    }

    /**
     * Inverse of {@link #toWorld}.
     *
     * <p><i>FR —</i> Inverse de {@link #toWorld}.
     *
     * @param p  block-space point / point dans l'espace du bloc
     * @param fx facing X step / pas X de l'orientation
     * @param fz facing Z step / pas Z de l'orientation
     * @return model point / point modèle
     */
    public static double[] toModel(double[] p, int fx, int fz) {
        double dx = p[0] - 0.5, dz = p[2] - 0.5;
        return new double[]{0.5 + dx * fz - dz * fx, p[1], 0.5 + dx * fx + dz * fz};
    }

    /**
     * Which key a look ray hits, in block space.
     *
     * <p><i>FR —</i> Quelle touche un rayon de visée touche, dans l'espace du bloc.
     *
     * @param origin ray start relative to the block corner (eye position) / départ du rayon relatif au coin du
     *               bloc (position des yeux)
     * @param dir    ray direction / direction du rayon
     * @param fx     facing X step / pas X de l'orientation
     * @param fz     facing Z step / pas Z de l'orientation
     * @return symbol id 1–38, {@link #DOME}, or {@link #NONE} / identifiant 1–38, {@link #DOME}, ou {@link #NONE}
     */
    public static int pick(double[] origin, double[] dir, int fx, int fz) {
        double[] o = toModel(origin, fx, fz);
        double[] d = toModel(new double[]{dir[0] + 0.5, dir[1], dir[2] + 0.5}, fx, fz);
        d[0] -= 0.5;
        d[2] -= 0.5;
        double denom = d[0] * NX + d[1] * NY + d[2] * NZ;
        if (Math.abs(denom) < 1e-9) {
            return NONE;
        }
        // Plane through the key tops. / Plan passant par le dessus des touches.
        double px = CX + KEY_HEIGHT * NX, py = CY + KEY_HEIGHT * NY, pz = CZ + KEY_HEIGHT * NZ;
        double t = ((px - o[0]) * NX + (py - o[1]) * NY + (pz - o[2]) * NZ) / denom;
        if (t < 0) {
            return NONE;
        }
        double hx = o[0] + t * d[0] - px, hy = o[1] + t * d[1] - py, hz = o[2] + t * d[2] - pz;
        double a = hx; // e1 = +X
        double b = hx * E2X + hy * E2Y + hz * E2Z;
        return pickOnConsole(Math.hypot(a, b), Math.atan2(b, a));
    }

    /**
     * Which key sits at polar position {@code (r, angle)} on the console.
     *
     * <p><i>FR —</i> Quelle touche se trouve à la position polaire {@code (r, angle)} sur la console.
     *
     * @param r     radius / rayon
     * @param angle radians from e1 / radians depuis e1
     * @return symbol id 1–38, {@link #DOME}, or {@link #NONE}
     */
    public static int pickOnConsole(double r, double angle) {
        if (r <= DOME_RADIUS) {
            return DOME;
        }
        boolean outer;
        if (r >= INNER_R0 && r <= INNER_R1) {
            outer = false;
        } else if (r >= OUTER_R0 && r <= OUTER_R1) {
            outer = true;
        } else {
            return NONE;
        }
        double step = 2 * Math.PI / KEYS_PER_RING;
        // index grows clockwise from the top / l'indice croît dans le sens horaire depuis le haut
        double fromTop = Math.PI / 2 - angle + step / 2;
        int index = (int) Math.floor(((fromTop % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI) / step);
        return symbolAt(outer, Math.min(index, KEYS_PER_RING - 1));
    }
}
