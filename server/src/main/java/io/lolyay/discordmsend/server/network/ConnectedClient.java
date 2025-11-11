package io.lolyay.discordmsend.server.network;

import com.sun.management.OperatingSystemMXBean;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.packet.Packet;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.PlayerUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.StatisticsS2CPacket;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.discordmsend.server.Server;
import io.lolyay.discordmsend.server.music.players.TrackPlayerClient;
import io.lolyay.discordmsend.server.music.players.TrackPlayerInstance;
import io.lolyay.discordmsend.util.logging.Logger;

import java.lang.management.ManagementFactory;

public class ConnectedClient {
    private final int protocolVersion;
    private final Connection connection;
    private final Server server;

    private TrackPlayerClient player;

    private long userId;

    private CUserData userData;

    public ConnectedClient(int protocolVersion, Connection connection, Server server) {
        this.protocolVersion = protocolVersion;
        this.connection = connection;
        this.server = server;
    }

    public Connection getConnection() {
        return connection;
    }

    public void updateClient(){
        if(!this.connection.isActive()){
            cleanup();
            server.removeClient(this);
        }
        if(this.connection.getPhase() == NetworkPhase.POST_ENCRYPTION)
            for(TrackPlayerInstance p : player.getPlayers().values()){
                connection.send(new PlayerUpdateS2CPacket(
                        p.getGuildId(),
                        p.isPaused(),
                        p.getVolume(),
                        p.getConsumer().getStreamer().getPlayingTrack() == null ? 0 : p.getConsumer().getStreamer().getPlayingTrack().getPosition(),
                        p.getConsumer().getStreamer().getPlayingTrack() != null,
                        server.unResolve(p.getConsumer().getStreamer().getPlayingTrack()).getTrackId()
                ));
            }
    }

    public void updateClientUsage(){
        if(!this.connection.isActive()) {
            cleanup();
            server.removeClient(this);
        }
        if(this.connection.getPhase() == NetworkPhase.POST_ENCRYPTION) {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;       // Max memory JVM can use
            long freeMemory = runtime.freeMemory() / 1024 / 1024;     // Free memory inside allocated memory
            com.sun.management.OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double processCpuLoad = osBean.getCpuLoad();
            int percent = (int) (processCpuLoad * 100);
            int players = player.getPlayers().size();
            int clients = server.getConnectedClients().size();
            connection.send(new StatisticsS2CPacket(maxMemory, freeMemory, percent, players, clients));
        }
    }

    public void setUserData(CUserData userData) {
        this.userData = userData;
    }

    public void setUserId(long userId){
        this.userId = userId;
        this.player = new TrackPlayerClient(String.valueOf(userId), server.getClient(), server, this);
        Logger.debug("Client " + userId + " Created SenderObject");
    }

    public void sendPacket(Packet<?> packet){
        if(!connection.isActive()) {
            cleanup();
            server.removeClient(this);
        }
        connection.send(packet);
    }

    public TrackPlayerClient getPlayer() {
        return player;
    }

    public int protocolVersion() {
        return protocolVersion;
    }
    
    public CUserData getUserData() {
        return userData;
    }
    
    /**
     * Cleanup all resources when client disconnects
     */
    public void cleanup() {
        if (player != null) {
            Logger.info("Cleaning up resources for disconnected client " + userId);
            // Stop and destroy all players
            for (TrackPlayerInstance p : player.getPlayers().values()) {
                try {
                    p.stop();
                    Logger.debug("Stopped player for guild " + p.getGuildId());
                } catch (Exception e) {
                    Logger.err("Error stopping player during cleanup: " + e.getMessage());
                }
            }
            player.getPlayers().clear();
        }
    }
}
