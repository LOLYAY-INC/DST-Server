package io.lolyay.discordmsend.client;

public class PlayerStatus {
    private final long guildId;
    private final boolean paused;
    private final int volume;
    private final long position;
    private final boolean hasTrack;
    private final int trackId;

    public long getGuildId() {
        return guildId;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getVolume() {
        return volume;
    }

    public long getPosition() {
        return position;
    }

    public boolean isHasTrack() {
        return hasTrack;
    }

    public int getTrackId() {
        return trackId;
    }

    public PlayerStatus(long guildId, boolean paused, int volume, long position, boolean hasTrack, int trackId) {
        this.guildId = guildId;
        this.paused = paused;
        this.volume = volume;
        this.position = position;
        this.hasTrack = hasTrack;
        this.trackId = trackId;
    }
}
