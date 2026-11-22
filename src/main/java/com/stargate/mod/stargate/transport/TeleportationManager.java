package com.stargate.mod.stargate.transport;

/**
 * Moves entities between gates. Empty for now: it will send what enters an open outgoing gate to the
 * gate given by {@link
 * com.stargate.mod.stargate.connection.ConnectionManager#getTravelDestination(java.util.UUID)}, across
 * dimensions if needed.
 *
 * <p><i>FR —</i> Déplace les entités entre les portes. Vide pour l'instant : il enverra ce qui entre
 * dans une porte sortante ouverte vers la porte donnée par {@link
 * com.stargate.mod.stargate.connection.ConnectionManager#getTravelDestination(java.util.UUID)}, y
 * compris vers une autre dimension.
 */
public class TeleportationManager {
}
