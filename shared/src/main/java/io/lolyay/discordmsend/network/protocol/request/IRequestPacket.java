package io.lolyay.discordmsend.network.protocol.request;

public interface IRequestPacket {
    int sequence();
    int getExchangeType();
}
