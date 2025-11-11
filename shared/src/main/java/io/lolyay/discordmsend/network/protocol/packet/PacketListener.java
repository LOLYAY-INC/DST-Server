package io.lolyay.discordmsend.network.protocol.packet;


import io.lolyay.discordmsend.network.protocol.Connection;

public interface PacketListener {
    // A common method to handle disconnection
    void onDisconnect(String reason);

    // Method to get the associated connection
    Connection getConnection();
}