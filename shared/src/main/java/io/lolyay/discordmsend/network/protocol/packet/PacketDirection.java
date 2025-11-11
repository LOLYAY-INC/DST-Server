package io.lolyay.discordmsend.network.protocol.packet;

/**
 * Represents the direction of traffic for a packet,
 * solving the issue of ID collisions between client-bound and server-bound packets.
 */
public enum PacketDirection {
    SERVER_BOUND, // Client-to-Server
    CLIENT_BOUND  // Server-to-Client
}