package io.lolyay.discordmsend.server.event;

import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.EncHelloC2SPacket;
import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.discordmsend.server.network.ConnectedClient;
import lombok.Getter;

@Getter
public class PostClientConnectEvent extends ClientEvent {
    private String cR;
    public PostClientConnectEvent(DstServer server, ConnectedClient client, EncHelloC2SPacket packet) {
        super(server, client);
    }

    public void setCancelled(boolean cancel, String reason) {
        super.setCancelled(cancel);
        this.cR = reason;
    }

    @Override
    public void setCancelled(boolean cancel) {
        super.setCancelled(cancel);
        this.cR = "Disconnected";
    }
}
