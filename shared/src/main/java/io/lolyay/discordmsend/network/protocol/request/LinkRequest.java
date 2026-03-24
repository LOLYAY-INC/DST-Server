package io.lolyay.discordmsend.network.protocol.request;

import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.RequestLinkC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.LinkResponseS2CPacket;

public class LinkRequest extends IRequest<RequestLinkC2SPacket, LinkResponseS2CPacket> {
    public static final int EXCHANGE_TYPE = 1;

    private final long guildId;

    public LinkRequest(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public RequestLinkC2SPacket createRequestPacket() {
        return new RequestLinkC2SPacket(guildId, sequenceGen.getAndIncrement());
    }

    @Override
    public Class<LinkResponseS2CPacket> getResponseTypeClass() {
        return LinkResponseS2CPacket.class;
    }
}
