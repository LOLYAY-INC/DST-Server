package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.obj.Severity;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import io.lolyay.discordmsend.server.music.pools.opus.OpusEncodingTask;
import io.lolyay.discordmsend.server.music.providers.IProvider;
import io.lolyay.discordmsend.server.music.providers.ProviderPool;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.BlockingQueue;


@Slf4j
public abstract class AbstractTrackConsumer {

    private OpusEncodingTask opusEncodingTask;

    @Getter(AccessLevel.PUBLIC)
    private final IProvider audioProvider;

    @Getter(AccessLevel.PROTECTED)
    private final GuildPlayerInstance playerInstance;

    @Getter(AccessLevel.PROTECTED)
    private final long guildId;

    @Setter
    @Getter
    private float volume = 1F;

    @Getter
    private boolean playing = false;

    @Getter
    protected boolean paused = false;

    private boolean stopped = true;


    protected abstract void init();

    protected abstract void start();

    protected abstract void cleanUp();

    protected abstract void end();
    public abstract void tick();

    protected boolean shouldEncodeToOpus() {
        return true;
    }

    public AbstractTrackConsumer(GuildPlayerInstance player, long guildId) {
        this.audioProvider = ProviderPool.getInstance().createProvider(player, player.getParent().getDstServer().getAudioCacheManager(), null);
        this.playerInstance = player;
        this.guildId = guildId;
        this.volume = playerInstance.getDefaultVolume() / 100F;
    }

    protected BlockingQueue<byte[]> getOpusQueue() {
        return playerInstance.getOpusQueue();
    }


    public final void stop() {
        if (stopped) {
            log.warn("Trying to stop already stopped player for guild {}", guildId);
            return;
        }
        stopped = true;

        audioProvider.stop();

        if (playing)
            resetInternal();

        end();
        log.info("Stopped and cleaned up player for guild {}", guildId);
    }

    public final void playTrack(TrackMetadata trackMetadata) {
        if (playing)
            reset();

        if (stopped)
            init();

        stopped = false;

        playerInstance.getPcmFrames().clear();
        playerInstance.getEncodePosition().set(0);
        playerInstance.getOpusQueue().clear();

        try {
            audioProvider.playTrack(trackMetadata, playerInstance);
        } catch (Exception e) {
            log.error("Failed to start audio provider for track '{}': {}", trackMetadata.trackName(), e.getMessage());
            playerInstance.onTrackFail(trackMetadata, Severity.FAULT, "Failed to start playback: " + e.getMessage());
            return;
        }

        playing = true;

        if (shouldEncodeToOpus()) {
            opusEncodingTask = playerInstance.getParent().getDstServer().getOpusEncoderPool()
                    .registerTask(playerInstance);
        }

        start();
    }

    public final void pause() {
        paused = true;
    }

    public final void resume() {
        paused = false;
    }


    protected final void reset() {
        resetInternal();
        audioProvider.cleanup();
    }

    private void resetInternal() {
        playing = false;

        if (opusEncodingTask != null && opusEncodingTask.isRunning()) {
            opusEncodingTask.stop();
            opusEncodingTask = null;
        }

        cleanUp();
        playerInstance.getPcmFrames().clear();
        playerInstance.getEncodePosition().set(0);
        playerInstance.getOpusQueue().clear();

        log.debug("Player {} cleared and ready for next track", guildId);
    }


    @Nullable
    public TrackMetadata getPlayingTrack() {
        return audioProvider.getPlayingTrack();
    }

    public long getPosition() {
        return audioProvider.getPosition();
    }
}
