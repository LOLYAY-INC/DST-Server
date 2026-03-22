package io.lolyay.discordmsend.server.event;

import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter
public abstract class ClientEvent extends DstEvent {
    private final ConnectedClient client;

    public ClientEvent(DstServer server, ConnectedClient client) {
        super(server);
        this.client = client;
    }

}
