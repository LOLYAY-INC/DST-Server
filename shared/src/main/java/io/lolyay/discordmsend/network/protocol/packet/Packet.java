package io.lolyay.discordmsend.network.protocol.packet;

public interface Packet<T extends PacketListener> {
    void apply(T listener);
}