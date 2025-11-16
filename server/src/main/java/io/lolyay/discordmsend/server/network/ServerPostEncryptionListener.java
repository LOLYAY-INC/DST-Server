package io.lolyay.discordmsend.server.network;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import io.lolyay.discordmsend.network.protocol.Connection;
import io.lolyay.discordmsend.network.protocol.listeners.server.ServerPostEncryptionPacketListener;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.*;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.discordmsend.obj.MusicTrack;
import io.lolyay.discordmsend.server.Server;
import io.lolyay.discordmsend.server.music.players.TrackPlayerInstance;
import io.lolyay.discordmsend.server.music.consumers.DiscordTrackConsumer;
import io.lolyay.discordmsend.util.logging.Logger;
import moe.kyokobot.koe.VoiceServerInfo;

public class ServerPostEncryptionListener implements ServerPostEncryptionPacketListener {
    private final Server server;
    private final Connection connection;
    private final ConnectedClient client;

    public ServerPostEncryptionListener(Server server, Connection connection, ConnectedClient client) {
        this.server = server;
        this.connection = connection;
        this.client = client;

        connection.send(new EncHelloS2CPacket(
                server.serverName(),
                server.serverVersion(),
                server.protocolVersion(),
                server.features(),
                server.ytSourceVersion(),
                server.moddedInfo(),
                server.countryCode()
        ));
        Logger.debug("Sent EncHelloS2CPacket");
    }

    @Override
    public void onEncHello(EncHelloC2SPacket packet) {
        Logger.success("Client \"%s\"s by \"%s\" Version \"%s\" Connected with ClientId %s".formatted(packet.userAgent(), packet.userAuthor(), packet.userVersion(), server.getConnectedClients().size()));
        Logger.info("Client with id %s has following ClientFeatures: ".formatted(server.getConnectedClients().size()) + packet.features());

        client.setUserData(new CUserData(packet.userAgent(),
                packet.userVersion(), packet.userAuthor(),
                packet.features()));
    }

    @Override
    public void onKeepAlive(KeepAliveC2SPacket packet) {
        getConnection().timeSinceLastPacket = System.currentTimeMillis(); // <-- update here
    }

    @Override
    public void onDiscordDetails(DiscordDetailsC2SPacket packet) {
        client.setUserId(packet.userId());
        Logger.debug("Received Discord Connection Details for " + packet.userId());
    }

    @Override
    public void onPlayerCreate(PlayerCreateC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId());
        Logger.debug("Received Player Create for " + packet.guildId());
    }

    @Override
    public void onPlayerConnect(PlayerConnectC2SPacket packet) {
        TrackPlayerInstance player = client.getPlayer().getOrCreatePlayer(packet.guildId());
        player.connect(new VoiceServerInfo(packet.sessionId(), packet.endPoint(), packet.token()));
        Logger.debug("Received Player Connect for " + packet.guildId());
    }

    @Override
    public void onSearch(SearchC2SPacket packet) {
        try {
            server.search((packet.music() ? "ytmsearch:" : "ytsearch:") + packet.query())
                .thenAccept(m -> {
                    if (m == null) {
                        Logger.err("Search failed: No matches found for " + packet.query());
                        return;
                    }
                    getConnection().send(new TrackSearchResponseS2CPacket(m.getTrackId(),packet.sequence()));
                    if(packet.details()){
                        AudioTrackInfo info = server.resolveTo(m).getInfo();
                        getConnection().send(new TrackDetailsS2CPacket(m.getTrackId(), info.title, info.author, info.artworkUrl == null ? "" : info.artworkUrl, info.length));
                    }
                })
                .exceptionally(ex -> {
                    Logger.err("Search failed: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onDetailsRequest(RequestTrackInfoC2SPacket packet) {
        AudioTrackInfo info = server.resolveTo(MusicTrack.ofId(packet.trackId())).getInfo();
        getConnection().send(new TrackDetailsS2CPacket(packet.trackId(), info.title, info.author, info.artworkUrl == null ? "" : info.artworkUrl, info.length));
    }

    @Override
    public void onPlayTrack(PlayTrackC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).playTrack(server.resolveTo(MusicTrack.ofId(packet.trackId())));
    }

    @Override
    public void onDefaultVolume(SetDefaultVolumeC2SPacket packet) {
        client.getPlayer().setDefaultVolume((float) packet.volume() / 100);
    }

    @Override
    public void onDisconnect(String reason) {
        Logger.info("Client disconnected, " + reason);
        server.networkServer().removeConnection(connection);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }


    @Override
    public void onPing(PingC2SPacket packet) {
        getConnection().send(new PongS2CPacket(packet.data()));
        Logger.debug("Pong!");
    }

    @Override
    public void onSearchMultiple(SearchMultipleC2SPacket packet) {
        try {
            server.searchMultiple((packet.music() ? "ytmsearch:" : "ytsearch:") + packet.query(), packet.maxResults()).thenAccept(m -> {
                getConnection().send(new SearchMultipleResponseS2CPacket(m.stream().map(MusicTrack::getTrackId).toList(), packet.sequence()));
                if(packet.details()){
                    for(MusicTrack musicTrack : m){
                        AudioTrackInfo info = server.resolveTo(musicTrack).getInfo();
                        getConnection().send(new TrackDetailsS2CPacket(musicTrack.getTrackId(), info.title, info.author, info.artworkUrl == null ? "" : info.artworkUrl, info.length));

                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onChangeSource(SetSourceC2SPacket packet) {
        Logger.warn("SetSourceC2SPacket is DEPRECATED and will be removed in a future version. " +
                "Only HighQualityOpusStreamer is supported now. Guild: " + packet.guildId());
        //This warning will stay until the plugin api is released
        // Log at info level for debugging
        Logger.info("Received SetSourceC2SPacket (deprecated) - Guild: " + 
                   packet.guildId() + 
                   ". Only HighQualityOpusStreamer is supported now.");
        
        // No action needed as we only use HighQualityOpusStreamer
    }

    @Override
    public void onTrackInject(InjectTrackC2SPacket packet) {
        Logger.debug("Injecting track...");
        if(packet.skipSearch()){
            try {
                server.addTrack(packet.trackUri())
                        .thenAccept(m -> {
                            if (m == null) {
                                Logger.err("Track inject failed: No matches found for " + packet.trackUri());
                                return;
                            }
                            getConnection().send(new TrackSearchResponseS2CPacket(m.getTrackId(), packet.sequence()));
                            Logger.debug("Track injected " + m.getTrackId());
                            if (packet.details()) {
                                AudioTrackInfo info = server.resolveTo(m).getInfo();
                                getConnection().send(new TrackDetailsS2CPacket(m.getTrackId(), info.title, info.author, info.artworkUrl == null ? "" : info.artworkUrl, info.length));
                            }
                        })
                        .exceptionally(ex -> {
                            Logger.err("Track inject failed: " + ex.getMessage());
                            ex.printStackTrace();
                            return null;
                        });
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            try {
                server.search(packet.trackUri())
                        .thenAccept(m -> {
                            if (m == null) {
                                Logger.err("Track inject failed: No matches found for " + packet.trackUri());
                                return;
                            }
                            getConnection().send(new TrackSearchResponseS2CPacket(m.getTrackId(), packet.sequence()));
                            Logger.debug("Track injected " + m.getTrackId());
                            if (packet.details()) {
                                AudioTrackInfo info = server.resolveTo(m).getInfo();
                                getConnection().send(new TrackDetailsS2CPacket(m.getTrackId(), info.title, info.author, info.artworkUrl == null ? "" : info.artworkUrl, info.length));
                            }
                        })
                        .exceptionally(ex -> {
                            Logger.err("Track inject failed: " + ex.getMessage());
                            ex.printStackTrace();
                            return null;
                        });
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onPlayerPause(PlayerPauseC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).pause();
    }

    @Override
    public void onPlayerResume(PlayerResumeC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).resume();
    }

    @Override
    public void onPlayerStop(PlayerStopC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).stop();
    }

    @Override
    public void onPlayerSetVolume(PlayerSetVolumeC2SPacket packet) {
        client.getPlayer().getOrCreatePlayer(packet.guildId()).setVolume((float) packet.volume() / 100);
    }
    
    @Override
    public void onRequestLink(RequestLinkC2SPacket packet) {
        long guildId = packet.guildId();
        int sequence = packet.sequence();
        
        // Check if upload system is initialized
        if (server.getTrackUploadHandler() == null) {
            connection.send(LinkResponseS2CPacket.error(guildId, sequence, 
                "Upload system not initialized. S3 credentials may be missing."));
            Logger.warn("Link request failed: Upload system not initialized");
            return;
        }
        
        try {
            TrackPlayerInstance player = client.getPlayer().getOrCreatePlayer(guildId);
            
            // Check if a track is playing
            if (player.getConsumer().getStreamer().getPlayingTrack() == null) {
                connection.send(LinkResponseS2CPacket.error(guildId, sequence, 
                    "No track currently playing"));
                Logger.debug("Link request failed: No track playing");
                return;
            }
            
            String trackUri = player.getConsumer().getStreamer().getPlayingTrack().getInfo().uri;
            String cacheId = server.getAudioCacheManager().computeHash(trackUri);
            
            // Check if PCM file exists
            if (!server.getAudioCacheManager().hasTrack(trackUri)) {
                connection.send(LinkResponseS2CPacket.error(guildId, sequence, 
                    "Track not cached. Please wait for track to finish playing first."));
                Logger.debug("Link request failed: Track not cached for " + cacheId);
                return;
            }
            
            // Get the PCM file path 
            String pcmFilePath = "./cache/tracks/" + cacheId + ".pcm";
            
            Logger.debug("Processing link request for cache ID: " + cacheId + " (sequence: " + sequence + ")");
            
            // Start upload process (or get cached URL)
            server.getTrackUploadHandler().getOrUploadTrack(cacheId, pcmFilePath)
                .thenAccept(url -> {
                    connection.send(LinkResponseS2CPacket.success(guildId, sequence, url));
                    Logger.debug("Link request successful: " + url);
                })
                .exceptionally(ex -> {
                    connection.send(LinkResponseS2CPacket.error(guildId, sequence, 
                        "Upload failed: " + ex.getMessage()));
                    Logger.err("Link request upload failed: " + ex.getMessage());
                    return null;
                });
                
        } catch (Exception e) {
            connection.send(LinkResponseS2CPacket.error(guildId, sequence, 
                "Internal error: " + e.getMessage()));
            Logger.err("Link request error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onForceReconnect(ForceReconnectC2SPacket packet) {
        TrackPlayerInstance player = client.getPlayer().getOrCreatePlayer(packet.guildId());
        
        // Check if this is a Discord connection
        if (player.getConsumer() instanceof DiscordTrackConsumer) {
            DiscordTrackConsumer dcConsumer = (DiscordTrackConsumer) player.getConsumer();
            dcConsumer.getConnection().stopAudioFramePolling();
            dcConsumer.getConnection().reconnect();
            dcConsumer.getConnection().startAudioFramePolling();
            Logger.debug("Force reconnected Discord player for guild " + packet.guildId());
        } else {
            Logger.warn("Force reconnect requested for non-Discord player (guild " + packet.guildId() + "). Ignoring.");
            //This is possible to implement ( restart generator and sender thread and clear pregen buffer but why? It shouldnt break unlike discord)
        }
    }
}
