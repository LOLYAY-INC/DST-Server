package io.lolyay.discordmsend.network.protocol.request;

import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.RequestTrackInfoC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackDetailsS2CPacket;

public class TrackInfoRequest extends IRequest<RequestTrackInfoC2SPacket, TrackDetailsS2CPacket> {
    public static final int EXCHANGE_TYPE = 2;

    private final int trackId;

    public TrackInfoRequest(int trackId) {
        this.trackId = trackId;
    }

    @Override
    public RequestTrackInfoC2SPacket createRequestPacket() {
        return new RequestTrackInfoC2SPacket(trackId);
    }

    @Override
    public Class<TrackDetailsS2CPacket> getResponseTypeClass() {
        return TrackDetailsS2CPacket.class;
    }
}
