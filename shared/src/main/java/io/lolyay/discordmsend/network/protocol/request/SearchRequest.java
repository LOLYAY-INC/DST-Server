package io.lolyay.discordmsend.network.protocol.request;

import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.SearchMultipleC2SPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.SearchResponseS2CPacket;

public class SearchRequest extends IRequest<SearchMultipleC2SPacket, SearchResponseS2CPacket> {
    public static final int EXCHANGE_TYPE = 0;


    private final String query;
    private final boolean details;
    private final int maxResults;

    public SearchRequest(String query, boolean details, int maxResults) {
        this.query = query;
        this.details = details;
        this.maxResults = maxResults;
    }

    @Override
    public SearchMultipleC2SPacket createRequestPacket() {
        return new SearchMultipleC2SPacket(query, sequenceGen.getAndIncrement(), details, maxResults);
    }

    @Override
    public Class<SearchResponseS2CPacket> getResponseTypeClass() {
        return SearchResponseS2CPacket.class;
    }
}
