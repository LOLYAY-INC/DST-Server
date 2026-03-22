package io.lolyay.discordmsend.server.music.consumers;

import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.kyokobot.koe.KoeEventListener;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.codec.CodecInstance;
import moe.kyokobot.koe.internal.json.JsonObject;
import moe.kyokobot.koe.media.AudioFrameProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Discord voice consumer.
 *
 * <p>Koe calls {@link #canProvide()} / {@link #provideFrame(ByteBuf)} on its own
 * internal schedule (every ~20ms). This consumer is therefore a purely reactive
 * "dumb sender": it just polls the opus queue that the encoder fills.
 *
 * <p>{@link #tick()} is a no-op because the Koe frame-polling loop already drives
 * the sending at the correct rate.
 */
@Slf4j
public class DiscordTrackConsumer extends AbstractTrackConsumer implements AudioFrameProvider, KoeEventListener {

    @Getter
    private final MediaConnection connection;

    public DiscordTrackConsumer(MediaConnection connection, GuildPlayerInstance player) {
        super(player, player.getGuildId());
        this.connection = connection;
        connection.setAudioSender(this);
        connection.registerListener(this);
    }

    // ── AbstractTrackConsumer hooks ──────────────────────────────────────────

    @Override
    protected void init() {
        log.debug("DiscordTrackConsumer init for guild {}", getGuildId());
    }

    @Override
    protected void start() {
        log.debug("Starting Discord audio frame polling for guild {}", getGuildId());
        connection.startAudioFramePolling();
    }

    @Override
    protected void cleanUp() {
        log.debug("DiscordTrackConsumer cleanUp for guild {}", getGuildId());
        getOpusQueue().clear();
    }

    @Override
    protected void end() {
        log.debug("Stopping Discord audio frame polling for guild {}", getGuildId());
        connection.stopAudioFramePolling();
    }

    /**
     * No-op: Koe drives the frame loop internally via {@link #provideFrame(ByteBuf)}.
     */
    @Override
    public void tick() {
        // Deliberately empty — Koe calls provideFrame() on its own schedule.
    }

    // ── AudioFrameProvider ───────────────────────────────────────────────────

    @Override
    public boolean canProvide() {
        return !getOpusQueue().isEmpty();
    }

    @Override
    public boolean provideFrame(ByteBuf targetBuffer) {
        byte[] frame = getOpusQueue().poll();
        if (frame == null) return false;
        targetBuffer.writeBytes(frame);
        return true;
    }

    @Override
    public void dispose() {
        stop();
    }

    // ── KoeEventListener ─────────────────────────────────────────────────────

    @Override
    public void onCodecChanged(@NotNull CodecInstance codecInstance) {
        log.debug("Discord Codec Changed: {}", codecInstance.getName());
    }

    @Override
    public void gatewayError(Throwable throwable) {
        log.error("Gateway Error for client {}",
                getPlayerInstance().getParent().getOwner().getUserData().userAgent(), throwable);
    }

    @Override
    public void gatewayReady(InetSocketAddress address, int i) {
        log.info("Gateway Ready for guild {}!", getGuildId());
    }

    @Override
    public void gatewayClosed(int i, @Nullable String s, boolean b) {
        log.error("Gateway Closed for guild {}, code: {}, reason: {}, remote: {}", getGuildId(), i, s, b);
    }

    @Override public void userStreamsChanged(String s, int i, int i1, int i2) {}
    @Override public void usersConnected(List<String> list) {}
    @Override public void userDisconnected(String s) {}
    @Override public void externalIPDiscovered(InetSocketAddress address) {}
    @Override public void sessionDescription(JsonObject jsonObject) {}
}
