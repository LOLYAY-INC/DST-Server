package io.lolyay.discordmsend.server.music.providers;

import io.lolyay.discordmsend.network.types.TrackMetadata;
import io.lolyay.discordmsend.server.music.players.GuildPlayerInstance;

import java.util.List;

public abstract class IProvider {

    /** Start decoding {@code track} and streaming PCM frames into the player's pcmFrames list. */
    public abstract void playTrack(io.lolyay.discordmsend.network.types.TrackMetadata track, GuildPlayerInstance player) throws Exception;

    /**
     * Stop playback immediately. Fires {@code onTrackEnd(STOPPED)} if a track was playing.
     * Must be idempotent.
     */
    public abstract void stop();

    public abstract String getId();

    /**
     * Signal that the current track is being replaced.
     * Fires {@code onTrackEnd(REPLACED)} and resets internal state.
     */
    public abstract void cleanup();

    /** @return true if the provider has finished writing all PCM frames for the current track. */
    public abstract boolean isStreamEnded();

    /** @return the currently playing track metadata, or {@code null} if idle. */
    public abstract TrackMetadata getPlayingTrack();

    /** @return number of PCM frames written so far for the current track. */
    public abstract long getPosition();

    /** @return true if this provider supports seek operations. */
    public abstract boolean supportsSeek();
}
