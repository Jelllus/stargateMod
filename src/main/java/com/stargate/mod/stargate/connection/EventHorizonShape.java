package com.stargate.mod.stargate.connection;

/**
 * Shape of the event horizon over time, independent of rendering: how far the surface bulges out along the
 * gate's normal at a given point, how wide it is, and how opaque. Pure math, so it can be tested and tuned
 * without the game.
 *
 * <p>Coordinates are polar on the gate plane: {@code rn} is the normalised radius (0 = centre, 1 = rim) and
 * {@code theta} the angle in radians. Offsets are in blocks, positive towards the front of the gate.
 *
 * <h2>Phases</h2>
 * <ul>
 *     <li>{@link WormholeState#ENGAGING} — the <em>kawoosh</em>: the surface appears, an unstable vortex
 *     bursts {@value #KAWOOSH_LENGTH} blocks forward, collapses back into the ring, and the surface settles
 *     with large ripples.</li>
 *     <li>{@link WormholeState#OPEN} — gentle ripples spreading from the centre; leftover ripples from the
 *     kawoosh fade out.</li>
 *     <li>{@link WormholeState#CLOSING} — the surface shrinks into the centre and fades.</li>
 * </ul>
 *
 * <p><i>FR —</i> Forme de l'horizon des événements au fil du temps, indépendante du rendu : de combien la
 * surface se bombe le long de la normale de la porte en un point donné, quelle est sa largeur et son
 * opacité. Calcul pur, donc testable et réglable sans le jeu.
 *
 * <p>Les coordonnées sont polaires dans le plan de la porte : {@code rn} est le rayon normalisé (0 = centre,
 * 1 = bord) et {@code theta} l'angle en radians. Les décalages sont en blocs, positifs vers l'avant de la
 * porte.
 *
 * <h2>Phases</h2>
 * <ul>
 *     <li>{@link WormholeState#ENGAGING} — le <em>kawoosh</em> : la surface apparaît, un vortex instable
 *     jaillit de {@value #KAWOOSH_LENGTH} blocs vers l'avant, se résorbe dans l'anneau, et la surface se
 *     stabilise avec de grandes ondulations.</li>
 *     <li>{@link WormholeState#OPEN} — ondulations douces partant du centre ; les ondulations restantes du
 *     kawoosh s'estompent.</li>
 *     <li>{@link WormholeState#CLOSING} — la surface se résorbe vers le centre et s'efface.</li>
 * </ul>
 */
public final class EventHorizonShape {
    /**
     * Radius of the horizon in blocks, just inside the ring.
     * / <i>FR —</i> Rayon de l'horizon en blocs, juste à l'intérieur de l'anneau.
     */
    public static final float RADIUS = 2.32f;

    /**
     * How far the kawoosh reaches in front of the gate, in blocks.
     * / <i>FR —</i> Portée du kawoosh devant la porte, en blocs.
     */
    public static final float KAWOOSH_LENGTH = 4.5f;

    // Kawoosh timeline, as fractions of the ENGAGING phase. / Chronologie du kawoosh, en fractions de la phase ENGAGING.
    private static final float FORM_END = 0.08f;
    private static final float BURST_PEAK = 0.35f;
    private static final float BURST_END = 0.70f;

    private EventHorizonShape() {
    }

    /**
     * Whether a surface is drawn in this state.
     *
     * <p><i>FR —</i> Indique si une surface est dessinée dans cet état.
     *
     * @param state gate state / état de la porte
     * @return {@code true} for ENGAGING, OPEN and CLOSING / {@code true} pour ENGAGING, OPEN et CLOSING
     */
    public static boolean isVisible(WormholeState state) {
        return state == WormholeState.ENGAGING || state == WormholeState.OPEN || state == WormholeState.CLOSING;
    }

    /**
     * Length of the kawoosh jet at a point of the ENGAGING phase.
     *
     * <p><i>FR —</i> Longueur du jet du kawoosh à un instant de la phase ENGAGING.
     *
     * @param progress 0 to 1 through the phase / de 0 à 1 dans la phase
     * @return blocks / blocs
     */
    public static float kawooshLength(float progress) {
        float p = clamp01(progress);
        if (p < FORM_END || p >= BURST_END) {
            return 0f;
        }
        if (p < BURST_PEAK) {
            return KAWOOSH_LENGTH * easeOut((p - FORM_END) / (BURST_PEAK - FORM_END));
        }
        return KAWOOSH_LENGTH * (1f - smoothstep((p - BURST_PEAK) / (BURST_END - BURST_PEAK)));
    }

    /**
     * Shape of the kawoosh across the horizon: 1 at the centre, 0 at the rim. Multiply by
     * {@link #kawooshLength(float)} to get how far it reaches at a given radius. Shared by the renderer and the
     * lethal zone, so what you see is what kills.
     *
     * <p><i>FR —</i> Profil du kawoosh sur l'horizon : 1 au centre, 0 au bord. À multiplier par
     * {@link #kawooshLength(float)} pour obtenir sa portée à un rayon donné. Partagé par le rendu et la zone
     * mortelle : ce qu'on voit est ce qui tue.
     *
     * @param rn normalised radius, 0 to 1 / rayon normalisé, de 0 à 1
     * @return 0 to 1
     */
    public static float kawooshProfile(float rn) {
        return (float) Math.pow(Math.max(0f, 1f - rn * rn), 1.5);
    }

    /**
     * Fraction of {@link #RADIUS} the surface covers: grows in as the vortex forms, shrinks out on closing.
     *
     * <p><i>FR —</i> Fraction de {@link #RADIUS} couverte par la surface : elle s'étend à la formation du
     * vortex et se résorbe à la fermeture.
     *
     * @param state       gate state / état de la porte
     * @param age         ticks in that state / ticks dans cet état
     * @param engageTicks length of ENGAGING / durée de ENGAGING
     * @param closeTicks  length of CLOSING / durée de CLOSING
     * @return 0 to 1
     */
    public static float radiusScale(WormholeState state, float age, int engageTicks, int closeTicks) {
        return switch (state) {
            case ENGAGING -> easeOut(clamp01(age / engageTicks / FORM_END));
            case CLOSING -> 1f - smoothstep(clamp01(age / closeTicks));
            case OPEN -> 1f;
            default -> 0f;
        };
    }

    /**
     * Surface opacity, 0 to 1.
     *
     * <p><i>FR —</i> Opacité de la surface, de 0 à 1.
     *
     * @param state      gate state / état de la porte
     * @param age        ticks in that state / ticks dans cet état
     * @param closeTicks length of CLOSING / durée de CLOSING
     * @return 0 to 1
     */
    public static float alpha(WormholeState state, float age, int closeTicks) {
        return switch (state) {
            case ENGAGING, OPEN -> 0.92f;
            case CLOSING -> 0.92f * (1f - clamp01(age / closeTicks) * clamp01(age / closeTicks));
            default -> 0f;
        };
    }

    /**
     * Displacement of the surface along the gate normal.
     *
     * <p><i>FR —</i> Déplacement de la surface le long de la normale de la porte.
     *
     * @param state       gate state / état de la porte
     * @param age         ticks in that state / ticks dans cet état
     * @param engageTicks length of ENGAGING / durée de ENGAGING
     * @param closeTicks  length of CLOSING / durée de CLOSING
     * @param rn          normalised radius, 0 to 1 / rayon normalisé, de 0 à 1
     * @param theta       angle in radians / angle en radians
     * @return blocks, positive towards the front / blocs, positif vers l'avant
     */
    public static float offset(WormholeState state, float age, int engageTicks, int closeTicks, float rn, float theta) {
        float edge = 1f - rn; // ripples die out at the rim / les ondulations s'éteignent au bord
        switch (state) {
            case ENGAGING -> {
                float p = clamp01(age / engageTicks);
                float jet = kawooshLength(p);
                float bulb = kawooshProfile(rn);
                float turbulence = jet * 0.12f * (float) Math.sin(6 * theta + 9 * rn - age * 0.8f) * rn * edge;
                float rippleAmp = p < BURST_END ? 0.35f : lerp(0.35f, 0.12f, (p - BURST_END) / (1f - BURST_END));
                float ripple = rippleAmp * (float) Math.sin(9 * rn - age * 0.6f) * edge;
                return jet * bulb + turbulence + ripple;
            }
            case OPEN -> {
                float residual = 0.12f * (float) Math.exp(-age / 30f);
                float ripple = (0.05f + residual) * (float) Math.sin(8 * rn - age * 0.35f) * edge;
                float wobble = 0.02f * (float) Math.sin(3 * theta + age * 0.1f) * rn * edge;
                return ripple + wobble;
            }
            case CLOSING -> {
                float p = clamp01(age / closeTicks);
                return (0.08f + 0.25f * p) * (float) Math.sin(10 * rn - age * 0.9f) * edge;
            }
            default -> {
                return 0f;
            }
        }
    }

    /**
     * Rotation of the surface texture, for a slow swirl.
     *
     * <p><i>FR —</i> Rotation de la texture de la surface, pour un tourbillon lent.
     *
     * @param time any steadily increasing time in ticks / un temps croissant quelconque, en ticks
     * @return radians
     */
    public static float swirl(float time) {
        return time * 0.012f;
    }

    private static float clamp01(float x) {
        return x < 0f ? 0f : Math.min(x, 1f);
    }

    private static float easeOut(float x) {
        float t = 1f - clamp01(x);
        return 1f - t * t * t;
    }

    private static float smoothstep(float x) {
        float t = clamp01(x);
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }
}
