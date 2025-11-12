package io.lolyay.discordmsend.server.music.players;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackTimingUpdateS2CPacket;
import io.lolyay.discordmsend.obj.EndReason;
import io.lolyay.discordmsend.obj.Severity;
import io.lolyay.discordmsend.server.music.providers.HighQualityOpusStreamer;
import io.lolyay.discordmsend.server.music.consumers.DiscordTrackConsumer;
import io.lolyay.discordmsend.server.music.consumers.ITrackConsumer;
import io.lolyay.discordmsend.server.music.consumers.PacketTrackConsumer;
import io.lolyay.discordmsend.util.logging.Logger;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.VoiceServerInfo;
import moe.kyokobot.koe.codec.OpusCodec;

/**
 * Handles audio playback for a Discord bot client (uses KOE voice connection)
 */
public class TrackPlayerInstance {
    private final TrackPlayerClient parent;
    private final ITrackConsumer consumer;
    private final boolean isDiscord;
    private boolean wasStarted = false;
    private final long guildId;

    public TrackPlayerInstance(long guildId, TrackPlayerClient parent, MediaConnection connection) {
        this.guildId = guildId;
        this.parent = parent;
        this.isDiscord = true;
        Logger.debug("Created Discord SenderPlayer");
        
        // Create streamer and set it as KOE audio sender
        try {
            HighQualityOpusStreamer streamer = new HighQualityOpusStreamer(this, parent.getServer().getAudioCacheManager());
            connection.setAudioCodec(OpusCodec.INSTANCE);
            DiscordTrackConsumer dcConsumer = new DiscordTrackConsumer(connection, this, streamer);
            connection.setAudioSender(dcConsumer);
            consumer = dcConsumer;
        } catch (Exception e) {
            Logger.err("Failed to create HighQualityOpusStreamer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public TrackPlayerInstance(long guildId, TrackPlayerClient parent) {
        this.guildId = guildId;
        this.parent = parent;
        this.isDiscord = false;
        Logger.debug("Created Not Discord SenderPlayer");
        // Create streamer and set it as KOE audio sender
        try {
            HighQualityOpusStreamer streamer = new HighQualityOpusStreamer(this, parent.getServer().getAudioCacheManager());
            PacketTrackConsumer packetTrackConsumer = new PacketTrackConsumer(this, streamer);
            this.consumer = packetTrackConsumer;
        } catch (Exception e) {
            Logger.err("Failed to create HighQualityOpusStreamer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public TrackPlayerClient getParent() {
        return parent;
    }

    public ITrackConsumer getConsumer() {
        return consumer;
    }

    public void connect(VoiceServerInfo info){
        if(!isDiscord){
            Logger.err("Tried to connect a non Discord Connection, Did you forget the IS_DISCORD_BOT ClientFeature?");
            return;
        }
        ((DiscordTrackConsumer) consumer).getConnection().connect(info);
    }

    public boolean isPaused(){
        return consumer.getStreamer().isPaused();
    }

    public void playTrack(AudioTrack track){
        if(!wasStarted){
            consumer.start();
            wasStarted = true;
        }
        consumer.undestroy();
        consumer.getStreamer().playTrack(track);
    }

    public void setVolume(float volume){
        consumer.getStreamer().setVolume(volume);
    }

    public int getVolume() {
        return (int) (consumer.getStreamer().getVolume() * 100);
    }

    public void stop(){
        consumer.stop();
    }

    public void pause(){
        consumer.getStreamer().pause();
    }

    public void resume(){
        consumer.getStreamer().resume();
    }

    // AudioPlayerCallback implementation
    public void onTrackStart(AudioTrack track) {
        int trackId = parent.getServer().unResolve(track).getTrackId();
        parent.getOwner().sendPacket(new PlayerTrackStartS2CPacket(getGuildId(), trackId));
        parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                TrackTimingUpdateS2CPacket.TrackTimingType.STARTED, getGuildId(), System.currentTimeMillis()));
    }

    public void onTrackEnd(AudioTrack track, EndReason reason) {
        int trackId = parent.getServer().unResolve(track).getTrackId();
        parent.getOwner().sendPacket(new PlayerTrackEndS2CPacket(getGuildId(), trackId, reason));
        if (reason == EndReason.STOPPED) {
            parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                    TrackTimingUpdateS2CPacket.TrackTimingType.STOPPED, getGuildId(), System.currentTimeMillis()));
        }
    }

    public void onTrackFail(AudioTrack track, Severity severity, String message) {
        int trackId = parent.getServer().unResolve(track).getTrackId();
        parent.getOwner().sendPacket(new PlayerTrackFailS2CPacket(getGuildId(), trackId, severity, message));
    }

    public void onPause() {
        parent.getOwner().sendPacket(new PlayerPauseS2CPacket(getGuildId()));
        parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                TrackTimingUpdateS2CPacket.TrackTimingType.PAUSED, getGuildId(), System.currentTimeMillis()));
    }

    public void onResume() {
        parent.getOwner().sendPacket(new PlayerResumeS2CPacket(getGuildId()));
        parent.getOwner().sendPacket(new TrackTimingUpdateS2CPacket(
                TrackTimingUpdateS2CPacket.TrackTimingType.RESUMED, getGuildId(), System.currentTimeMillis()));
    }

    public long getGuildId() {
        return this.guildId;
    }

    public float getDefaultVolume() {
        return parent.getDefaultVolume();
    }
}
