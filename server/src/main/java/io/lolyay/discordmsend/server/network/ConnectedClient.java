package io.lolyay.discordmsend.server.network;

import com.sun.management.OperatingSystemMXBean;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.PlayerUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.StatisticsS2CPacket;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.discordmsend.server.DstServer;
import io.lolyay.discordmsend.server.music.players.ConnectedPlayer;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Slf4j
public class ConnectedClient {
    private final int protocolVersion;
    private final Connection connection;
    private final DstServer dstServer;

    private ConnectedPlayer player;
    private CountDownLatch connectedPlayerLatch = new CountDownLatch(1);

    private CUserData userData;

    public ConnectedClient(int protocolVersion, Connection connection, DstServer dstServer) {
        this.protocolVersion = protocolVersion;
        this.connection = connection;
        this.dstServer = dstServer;
    }

    @SneakyThrows
    public ConnectedPlayer getPlayer() {
        if(!connectedPlayerLatch.await(1, TimeUnit.SECONDS))
            throw new RuntimeException("Couldnt Create Player!");
        else return player;
    }

    public void updateClient(){
        if(!this.connection.isActive()){
            cleanup();
            dstServer.removeClient(this);
        }
        if(this.connection.getPhase() == NetworkPhase.POST_ENCRYPTION)
            for(GuildPlayerInstance p : player.getPlayers().values()){
                connection.send(new PlayerUpdateS2CPacket(
                        p.getGuildId(),
                        p.isPaused(),
                        p.getVolume(),
                        p.getConsumer().getPlayingTrack() == null ? 0 : p.getPosition(),
                        p.getConsumer().getPlayingTrack() != null,
                        dstServer.unResolve(p.getConsumer().getPlayingTrack()).getTrackId()
                ));
            }
    }

    public void updateClientUsage(){
        if(!this.connection.isActive()) {
            cleanup();
            dstServer.removeClient(this);
        }
        if(this.connection.getPhase() == NetworkPhase.POST_ENCRYPTION) {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            com.sun.management.OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double processCpuLoad = osBean.getCpuLoad();
            int percent = (int) (processCpuLoad * 100);
            int players = player.getPlayers().size();
            int clients = dstServer.getConnectedClients().size();
            connection.send(new StatisticsS2CPacket(maxMemory, freeMemory, percent, players, clients));
        }
    }

    public void setUserId(long userId){
        this.player = new ConnectedPlayer(String.valueOf(userId), dstServer.getClient(), dstServer, this);
        log.debug("Client " + userId + " Created SenderObject");
        connectedPlayerLatch.countDown();
    }

    public void sendPacket(Packet<?> packet){
        if(!connection.isActive()) {
            cleanup();
            dstServer.removeClient(this);
        }
        connection.send(packet);
    }

    public void cleanup() {
        if (player != null) {
            log.info("Cleaning up resources for disconnected client " + player.getUserId());
            for (GuildPlayerInstance p : player.getPlayers().values()) {
                try {
                    p.stop();
                    log.debug("Stopped player for guild " + p.getGuildId());
                } catch (Exception e) {
                    log.error("Error stopping player during cleanup: " + e.getMessage());
                }
            }
            player.destroy();
        }
    }
}
