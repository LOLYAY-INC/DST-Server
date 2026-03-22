package io.lolyay.discordmsend.client;

public record PlayerStatus(long guildId, boolean paused, int volume, long position, boolean hasTrack, int trackId) {

}
