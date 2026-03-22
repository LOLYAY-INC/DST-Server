package io.lolyay.discordmsend.network.protocol.request;

public interface IResponsePacket {
    int sequence();
    int getExchangeType();
}
