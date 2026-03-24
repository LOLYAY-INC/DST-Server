package io.lolyay.discordmsend.client;

import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.AudioS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackTimingUpdateS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.events.*;

public interface ClientEventHandler {
    void onPlayerPause(PlayerPauseS2CPacket packet);

    void onPlayerResume(PlayerResumeS2CPacket packet);

    void onPlayerTrackStart(PlayerTrackStartS2CPacket packet);

    void onPlayerTrackEnd(PlayerTrackEndS2CPacket packet);

    void onPlayerTrackError(PlayerTrackFailS2CPacket packet);

    void onPlayerTrackStuck(PlayerTrackStuckS2CPacket packet);

    void onDisconnect(String reason);

    void onTrackTimingUpdate(TrackTimingUpdateS2CPacket packet);

    void onAudio(AudioS2CPacket packet);

}
