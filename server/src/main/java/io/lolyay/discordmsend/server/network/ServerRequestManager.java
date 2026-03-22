package io.lolyay.discordmsend.server.network;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.request.IRequestPacket;
import io.lolyay.discordmsend.network.protocol.request.IResponsePacket;
import io.lolyay.discordmsend.server.DstServer;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.util.concurrent.CompletableFuture;

public abstract class ServerRequestManager {
    private static final Int2ObjectArrayMap<IServerRequestWrapper<? extends IRequestPacket, ? extends IResponsePacket>> requests = new Int2ObjectArrayMap<>();

    public static <T extends IRequestPacket, R extends IResponsePacket & Packet<?>> void registerExchange(int exchangeId, IServerRequestHandler<T, R> exchangeFactory) {
        requests.put(exchangeId, new IServerRequestWrapper<>(exchangeFactory));
    }

    @SuppressWarnings("unchecked")
    public static <T extends IRequestPacket, R extends IResponsePacket & Packet<?>> IServerRequestWrapper<T,R> getExchange(int exchangeId) {
        return (IServerRequestWrapper<T, R>) requests.get(exchangeId);
    }

    public static class IServerRequestWrapper<T extends IRequestPacket, R extends IResponsePacket & Packet<?>> {
        private final IServerRequestHandler<T,R> handler;

        public IServerRequestWrapper(IServerRequestHandler<T,R> handler) {
            this.handler = handler;
        }

        @CanIgnoreReturnValue
        public CompletableFuture<R> handle(T packet, DstServer dstServer, ConnectedClient client) {
            CompletableFuture<R> future = new CompletableFuture<>();
            new Thread(() -> future.complete(handler.handle(packet, dstServer, client))).start();
            future.thenAccept(response -> client.getConnection().send(response));
            return future;
        }
    }
    @FunctionalInterface
    public interface IServerRequestHandler<T extends IRequestPacket, R extends IResponsePacket> {
        R handle(T packet, DstServer dstServer, ConnectedClient client);
    }
}
