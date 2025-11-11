package io.lolyay.discordmsend.client;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackTimingUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.ev.*;

public interface ClientEventHandler {
    public void onPlayerPause(PlayerPauseS2CPacket packet);

    public void onPlayerResume(PlayerResumeS2CPacket packet);

    public void onPlayerTrackStart(PlayerTrackStartS2CPacket packet);

    public void onPlayerTrackEnd(PlayerTrackEndS2CPacket packet);

    public void onPlayerTrackError(PlayerTrackFailS2CPacket packet);

    public void onPlayerTrackStuck(PlayerTrackStuckS2CPacket packet);

    public void onDisconnect(String reason);

    public void onTrackTimingUpdate(TrackTimingUpdateS2CPacket packet);

    /**
     * Called when an audio packet is received from the server.
     * This is sent by special guild IDs (0-200) that use direct packet transmission.
     * 
     * @param packet The audio packet containing guild ID and Opus-encoded audio data (20ms frame)
     */
    public void onAudio(AudioS2CPacket packet);

}
