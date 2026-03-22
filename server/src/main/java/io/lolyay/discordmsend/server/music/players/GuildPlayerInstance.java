package io.lolyay.discordmsend.server.music.players;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackTimingUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.events.*;
import io.lolyay.discordmsend.network.types.ClientFeatures;
import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.obj.EndReason;
import io.lolyay.discordmsend.obj.Severity;
import io.lolyay.discordmsend.server.music.consumers.packet.PcmPacketTrackConsumer;
import io.lolyay.discordmsend.server.music.consumers.DiscordTrackConsumer;
import io.lolyay.discordmsend.server.music.consumers.AbstractTrackConsumer;
import io.lolyay.discordmsend.server.music.consumers.packet.OpusPacketTrackConsumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.OpusCodecInfo;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Slf4j
public class GuildPlayerInstance {
    private final ConnectedPlayer parent;
    private final AbstractTrackConsumer consumer;
    private final long guildId;

    /**
     * Flat store of all decoded PCM frames for the current track.
     * Index 0 = first 20ms frame. Written by the audio provider's decode thread,
     * read by the encoder task.
     * Access: provider writes sequentially (single writer), encoder reads by index (single reader).
     * No synchronization needed for separate read/write heads.
     */
    private final ArrayList<short[]> pcmFrames = new ArrayList<>(3000);

    /**
     * Current encode position (frame index into {@link #pcmFrames}).
     * Atomic so {@link #seek(long)} is safe to call from any thread.
     */
    private final AtomicInteger encodePosition = new AtomicInteger(0);

    /**
     * Bounded queue from the smart encoder to the dumb sender.
     * Capacity = 750 frames ≈ 15 seconds of pre-encoded audio headroom.
     */
    private final BlockingQueue<byte[]> opusQueue = new ArrayBlockingQueue<>(100);


    // ── Constructors ─────────────────────────────────────────────────────────

    public GuildPlayerInstance(long guildId, ConnectedPlayer parent, MediaConnection connection) {
        this.guildId = guildId;
        this.parent = parent;
        log.debug("Created Discord GuildPlayerInstance for guild {}", guildId);
        connection.setAudioCodec(OpusCodecInfo.INSTANCE);
        consumer = new DiscordTrackConsumer(connection, this);
    }

    public GuildPlayerInstance(long guildId, ConnectedPlayer parent) {
        this.guildId = guildId;
        this.parent = parent;
        log.debug("Created packet GuildPlayerInstance for guild {}", guildId);
        if (parent.getOwner().getUserData().features().isEnabled(ClientFeatures.Feature.FORCE_OPUS))
            this.consumer = new OpusPacketTrackConsumer(this);
        else
            this.consumer = new PcmPacketTrackConsumer(this);
    }

    // ── Tick & seek ──────────────────────────────────────────────────────────

    /**
     * Called by {@link io.lolyay.discordmsend.server.music.pools.player.GuildPlayerPool}
     * on every loop iteration. Delegates to the consumer's tick.
     */
    public void tick() {
        consumer.tick();
    }

    /**
     * Seek to a specific frame index.
     * Sets the encoder head and discards any already-encoded frames in the queue
     * so the sender immediately starts from the new position.
     *
     * @param ms ms
     */
    public void seek(long ms) {
        int frameIndex = (int) ms / 1000;
        encodePosition.set(frameIndex);
        opusQueue.clear();
        log.debug("Seeked guild {} to frame {}", guildId, frameIndex);
    }

    /**
     * Atomically read the current position and advance it by 1.
     * Called by the encoder task to claim the next frame to encode.
     *
     * @return the frame index that was claimed
     */
    public int getAndIncrementPosition() {
        return encodePosition.getAndIncrement();
    }

    // ── Playback controls ────────────────────────────────────────────────────

    /** Expose the audio provider so the encoder pool can check streamEnded / getPlayingTrack. */
    public io.lolyay.discordmsend.server.music.providers.IProvider getAudioProvider() {
        return consumer.getAudioProvider();
    }

    public float getConsumerVolume() {
        return consumer.getVolume();
    }
    public long getPosition(){
        return encodePosition.get() * 20L;
    }

    public boolean isPaused() { return consumer.isPaused(); }
    public void playTrack(TrackMetadata track) { consumer.playTrack(track); }
    public void setVolume(float volume) { consumer.setVolume(volume); }
    public int getVolume() { return (int) (consumer.getVolume() * 100); }
    public void stop() { consumer.stop(); }
    public void pause() { consumer.pause(); onPause(); }
    public void resume() { consumer.resume(); onResume(); }
    public float getDefaultVolume() { return parent.getDefaultVolume(); }

    // ── Event callbacks ──────────────────────────────────────────────────────

    public void onTrackStart(TrackMetadata track) {
        int trackId = parent.getDstServer().unResolve(track).getTrackId();
        parent.getOwner().sendPacket(new PlayerTrackStartS2CPacket(getGuildId(), trackId));
        parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                TrackTimingUpdateS2CPacket.TrackTimingUpdateType.STARTED, getGuildId(), System.currentTimeMillis()));
    }

    public void onTrackEnd(TrackMetadata track, EndReason reason) {
        int trackId = parent.getDstServer().unResolve(track).getTrackId();
        parent.getOwner().sendPacket(new PlayerTrackEndS2CPacket(getGuildId(), trackId, reason));
        if (reason == EndReason.STOPPED) {
            parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                    TrackTimingUpdateS2CPacket.TrackTimingUpdateType.STOPPED, getGuildId(),
                    System.currentTimeMillis()));
        }
    }

    public void onTrackFail(TrackMetadata track, Severity severity, String message) {
        int trackId = parent.getDstServer().unResolve(track).getTrackId();
        parent.getOwner().sendPacket(new PlayerTrackFailS2CPacket(getGuildId(), trackId, severity, message));
    }

    public void onPause() {
        parent.getOwner().sendPacket(new PlayerPauseS2CPacket(getGuildId()));
        parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                TrackTimingUpdateS2CPacket.TrackTimingUpdateType.PAUSED, getGuildId(), System.currentTimeMillis()));
    }

    public void onResume() {
        parent.getOwner().sendPacket(new PlayerResumeS2CPacket(getGuildId()));
        parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                TrackTimingUpdateS2CPacket.TrackTimingUpdateType.RESUMED, getGuildId(), System.currentTimeMillis()));
    }
}
