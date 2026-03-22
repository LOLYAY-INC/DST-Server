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

/**
 * Base class for all audio consumers.
 *
 * <p>Data flow:
 * <pre>
 *   IProvider  →  playerInstance.pcmFrames[]  →  OpusEncodingTask
 *                                                       │
 *                                              playerInstance.opusQueue
 *                                                       │
 *                                               consumer.tick()  ("dumb sender")
 * </pre>
 *
 * <ul>
 *   <li>The <b>provider</b> decodes audio and writes {@code short[]} frames into
 *       {@code playerInstance.getPcmFrames()} sequentially.</li>
 *   <li>The <b>encoder task</b> (on {@link io.lolyay.discordmsend.server.music.pools.opus.OpusEncoderPool})
 *       reads frames by index, encodes them to Opus, and pushes to
 *       {@code playerInstance.getOpusQueue()}.</li>
 *   <li>The <b>consumer</b> (this class + subclass) is ticked by
 *       {@link io.lolyay.discordmsend.server.music.pools.player.GuildPlayerPool}.
 *       Each tick it drains {@code opusQueue} and forwards frames to the destination
 *       (Discord voice / network packet / etc.).</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractTrackConsumer {

    public static final int FRAME_SIZE = 960;            // 20ms @ 48kHz mono / channel
    public static final int FRAME_INTERVAL_MS = 20;

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

    // ── Abstract lifecycle hooks ─────────────────────────────────────────────

    /** Called before first playback, or after a full stop, to set up any required state. */
    protected abstract void init();

    /** Called once the encoder task has started and the opus queue is being filled. */
    protected abstract void start();

    /** Release per-track resources. Called between tracks (track replaced / track ended). */
    protected abstract void cleanUp();

    /** Full stop; called once on {@link #stop()}. */
    protected abstract void end();

    /**
     * Called by {@link io.lolyay.discordmsend.server.music.pools.player.GuildPlayerPool}
     * on every loop iteration.
     * Implementations should be the "dumb sender": poll {@link #getOpusQueue()}, forward
     * the frame to the destination, and return quickly. No heavy work here.
     */
    public abstract void tick();

    /**
     * Override and return {@code false} if this consumer handles raw PCM directly
     * (e.g. {@link io.lolyay.discordmsend.server.music.consumers.packet.PcmPacketTrackConsumer})
     * and does not need an Opus encoder task.
     */
    protected boolean shouldEncodeToOpus() {
        return true;
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public AbstractTrackConsumer(GuildPlayerInstance player, long guildId) {
        this.audioProvider = ProviderPool.getInstance().createProvider(player, player.getParent().getDstServer().getAudioCacheManager(), null);
        this.playerInstance = player;
        this.guildId = guildId;
        this.volume = playerInstance.getDefaultVolume() / 100F;
    }

    // ── Convenience accessor ─────────────────────────────────────────────────

    /**
     * The encoder→sender queue. Backed by {@link GuildPlayerInstance#getOpusQueue()}.
     */
    protected BlockingQueue<byte[]> getOpusQueue() {
        return playerInstance.getOpusQueue();
    }

    // ── Public playback API ──────────────────────────────────────────────────

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
            reset(); // fires REPLACED for the outgoing track

        if (stopped)
            init();

        stopped = false;

        // Clear all state from the previous track
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

    // ── Internal helpers ─────────────────────────────────────────────────────

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

    // ── Metadata passthrough ─────────────────────────────────────────────────

    @Nullable
    public TrackMetadata getPlayingTrack() {
        return audioProvider.getPlayingTrack();
    }

    public long getPosition() {
        return audioProvider.getPosition();
    }
}
