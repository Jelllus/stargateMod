package com.stargate.mod.stargate.connection;

import com.stargate.mod.stargate.address.StargateSymbol;

/**
 * Movement of a gate's inner ring (the symbol track) while dialing one symbol: the ring spins from
 * {@code from} to {@code to}, then the top chevron locks for {@code lockTicks}. Positions are in symbols:
 * position {@code p} means symbol {@code p} sits under the top chevron. {@code to} may be below {@code from}
 * (the ring turns backwards) or beyond {@value StargateSymbol#COUNT} (it wraps); use {@link #position} to read
 * it. A ring at rest has {@code from == to} and no duration.
 *
 * <p>Shared by the server (timing of each lock) and the client (animation), so both agree.
 *
 * <p><i>FR —</i> Mouvement de l'anneau intérieur d'une porte (la piste des symboles) pendant la numérotation
 * d'un symbole : l'anneau tourne de {@code from} à {@code to}, puis le chevron du haut se verrouille pendant
 * {@code lockTicks}. Les positions sont en symboles : la position {@code p} signifie que le symbole {@code p}
 * est sous le chevron du haut. {@code to} peut être inférieur à {@code from} (l'anneau tourne à l'envers) ou
 * dépasser {@value StargateSymbol#COUNT} (il fait le tour) ; lisez-le avec {@link #position}. Un anneau au repos
 * a {@code from == to} et aucune durée.
 *
 * <p>Partagé par le serveur (minutage de chaque verrouillage) et le client (animation), pour qu'ils
 * concordent.
 *
 * @param from      position at the start / position au départ
 * @param to        position at the end of the spin / position à la fin de la rotation
 * @param startTick tick the spin started (manager or game time) / tick de début (gestionnaire ou temps de jeu)
 * @param spinTicks spin duration / durée de la rotation
 * @param lockTicks lock duration after the spin / durée du verrouillage après la rotation
 */
public record RingMotion(double from, double to, long startTick, int spinTicks, int lockTicks) {
    /**
     * A ring at rest.
     *
     * <p><i>FR —</i> Un anneau au repos.
     *
     * @param position symbol under the top chevron / symbole sous le chevron du haut
     * @return the motion / le mouvement
     */
    public static RingMotion rest(double position) {
        return new RingMotion(position, position, 0, 0, 0);
    }

    /**
     * Plans the spin that brings {@code target} under the top chevron, turning forwards or backwards.
     *
     * <p><i>FR —</i> Prépare la rotation qui amène {@code target} sous le chevron du haut, vers l'avant ou
     * vers l'arrière.
     *
     * @param current        current position / position actuelle
     * @param target         symbol to bring to the top / symbole à amener en haut
     * @param forwards       turning direction / sens de rotation
     * @param startTick      start tick / tick de départ
     * @param ticksPerSymbol spin speed / vitesse de rotation
     * @param lockTicks      lock duration / durée du verrouillage
     * @return the motion / le mouvement
     */
    public static RingMotion towards(double current, StargateSymbol target, boolean forwards, long startTick,
                                     int ticksPerSymbol, int lockTicks) {
        double from = wrap(current);
        double distance = forwards ? wrap(target.id() - from) : wrap(from - target.id());
        int spin = (int) Math.round(distance * ticksPerSymbol);
        return new RingMotion(from, forwards ? from + distance : from - distance, startTick, spin, lockTicks);
    }

    /**
     * Position at {@code tick}, in [0, {@value StargateSymbol#COUNT}). Eases in and out.
     *
     * <p><i>FR —</i> Position au tick {@code tick}, dans [0, {@value StargateSymbol#COUNT}). Accélère puis
     * ralentit.
     *
     * @param tick current tick, fractional on the client / tick courant, fractionnaire côté client
     * @return the position / la position
     */
    public double position(double tick) {
        double t = spinTicks <= 0 ? 1 : Math.clamp((tick - startTick) / spinTicks, 0, 1);
        double eased = t * t * (3 - 2 * t);
        return wrap(from + (to - from) * eased);
    }

    /**
     * Tick the spin ends and the top chevron starts locking.
     *
     * <p><i>FR —</i> Tick où la rotation se termine et où le chevron du haut commence à se verrouiller.
     *
     * @return the tick / le tick
     */
    public long spinEnd() {
        return startTick + spinTicks;
    }

    /**
     * Tick the symbol is locked.
     *
     * <p><i>FR —</i> Tick où le symbole est verrouillé.
     *
     * @return the tick / le tick
     */
    public long lockEnd() {
        return spinEnd() + lockTicks;
    }

    /**
     * Whether the top chevron is locking at {@code tick}.
     *
     * <p><i>FR —</i> Indique si le chevron du haut se verrouille au tick {@code tick}.
     *
     * @param tick the tick / le tick
     * @return {@code true} while locking / {@code true} pendant le verrouillage
     */
    public boolean isLocking(double tick) {
        return lockTicks > 0 && tick >= spinEnd() && tick < lockEnd();
    }

    /**
     * The same motion with its start moved by {@code delta} ticks (e.g. manager time → game time).
     *
     * <p><i>FR —</i> Le même mouvement avec son départ décalé de {@code delta} ticks (par exemple temps du
     * gestionnaire → temps de jeu).
     *
     * @param delta shift / décalage
     * @return the shifted motion / le mouvement décalé
     */
    public RingMotion shifted(long delta) {
        return new RingMotion(from, to, startTick + delta, spinTicks, lockTicks);
    }

    private static double wrap(double p) {
        double n = StargateSymbol.COUNT;
        return ((p % n) + n) % n;
    }
}
