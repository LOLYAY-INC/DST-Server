
package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.server.music.players.TrackPlayerInstance;
import io.lolyay.discordmsend.server.music.providers.HighQualityOpusStreamer;
import io.lolyay.discordmsend.util.logging.Logger;
import io.netty.buffer.ByteBuf;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.media.OpusAudioFrameProvider;

public class DiscordTrackConsumer extends OpusAudioFrameProvider implements ITrackConsumer {
    private final TrackPlayerInstance player;
    private final HighQualityOpusStreamer currentProvider;
    private final MediaConnection connection;

    public DiscordTrackConsumer(MediaConnection connection, TrackPlayerInstance player, HighQualityOpusStreamer currentProvider) {
        super(connection);
        this.connection = connection;
        this.player = player;
        connection.setAudioSender(this);
        this.currentProvider = currentProvider;
    }

    public MediaConnection getConnection() {
        return connection;
    }

    @Override
    public boolean canProvide() {
        return currentProvider != null && currentProvider.canProvide();
    }

    @Override
    public void retrieveOpusFrame(ByteBuf targetBuffer) {
        if (currentProvider == null) {
            return;
        }

        byte[] opusFrame = currentProvider.provide20MsOpus();
        if (opusFrame != null) {
            targetBuffer.writeBytes(opusFrame);
        }
    }

    @Override
    public void start() {
        connection.startAudioFramePolling();
    }

    /**
     * Shutdown and cleanup all resources
     */
    public void stop() {
        if (currentProvider != null) {
            currentProvider.stop();
        }
        connection.stopAudioFramePolling();
        Logger.info("FrameProvider shutdown complete");
    }

    @Override
    public HighQualityOpusStreamer getStreamer() {
        return currentProvider;
    }
}
