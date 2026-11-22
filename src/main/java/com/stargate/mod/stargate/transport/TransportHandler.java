package com.stargate.mod.stargate.transport;

/**
 * Decides what happens to an entity touching an event horizon. Empty for now: it will kill entities
 * entering an {@link com.stargate.mod.stargate.connection.WormholeState#ENGAGING ENGAGING} vortex,
 * block entry from the incoming side, and hand the rest to {@link TeleportationManager}.
 *
 * <p><i>FR —</i> Décide du sort d'une entité qui touche un horizon des événements. Vide pour l'instant
 * : il tuera les entités qui entrent dans un vortex {@link
 * com.stargate.mod.stargate.connection.WormholeState#ENGAGING ENGAGING}, bloquera l'entrée côté
 * entrant, et confiera le reste à {@link TeleportationManager}.
 */
public class TransportHandler {
}
