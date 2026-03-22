package io.lolyay.discordmsend.network.protocol.packet;


import io.lolyay.discordmsend.network.protocol.Connection;

public interface PacketListener {
    void onDisconnect(String reason);
    Connection getConnection();
}