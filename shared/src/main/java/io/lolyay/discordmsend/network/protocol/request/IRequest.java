package io.lolyay.discordmsend.network.protocol.request;

import io.lolyay.discordmsend.network.protocol.packet.Packet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class IRequest<T extends IRequestPacket, R extends IResponsePacket> {
    protected final static AtomicInteger sequenceGen = new AtomicInteger(Integer.MIN_VALUE);
    private final CompletableFuture<R> future = new CompletableFuture<>();

    public abstract T createRequestPacket();

    public void accept(R responsePacket){
        future.complete(responsePacket);
    }

    public CompletableFuture<R> future() {
        return future;
    }

    public abstract Class<R> getResponseTypeClass();


}
