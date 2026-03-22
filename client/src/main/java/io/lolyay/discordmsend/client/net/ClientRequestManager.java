package io.lolyay.discordmsend.client.net;

import io.lolyay.discordmsend.network.protocol.request.IRequest;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;
import io.lolyay.discordmsend.network.protocol.request.IResponsePacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRequestManager {

    private final Map<Integer, Map<Integer, IRequest<?, ?>>> pendingRequests = new ConcurrentHashMap<>();

    public <T extends IRequestPacket, R extends IResponsePacket> T createAndRegister(IRequest<T, R> request) {
        T requestPacket = request.createRequestPacket();
        pendingRequests
                .computeIfAbsent(requestPacket.getExchangeType(), ignored -> new ConcurrentHashMap<>())
                .put(requestPacket.sequence(), request);
        return requestPacket;
    }

    public boolean handleResponse(IResponsePacket responsePacket) {
        int exchangeType = responsePacket.getExchangeType();
        Map<Integer, IRequest<?, ?>> sequence2Request = pendingRequests.get(exchangeType);
        if (sequence2Request == null) {
            return false;
        }

        IRequest<?, ?> request = sequence2Request.remove(responsePacket.sequence());
        if (request == null) {
            return false;
        }

        if (sequence2Request.isEmpty()) {
            pendingRequests.remove(exchangeType, sequence2Request);
        }

        acceptResponseUnsafe(request, responsePacket);
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void acceptResponseUnsafe(IRequest request, IResponsePacket responsePacket) {
        Class responseClass = request.getResponseTypeClass();
        if (!responseClass.isInstance(responsePacket)) {
            throw new IllegalArgumentException(
                    "Response type mismatch. Expected " + responseClass.getName() + " but got " + responsePacket.getClass().getName()
            );
        }
        request.accept(responsePacket);
    }
}
