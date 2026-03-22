package io.lolyay.discordmsend.network.types;

public record TrackMetadata(
        int id,
        String trackName,
        String author,
        String art,
        String trackUrl,
        String isrc,
        long durationMs,
        String sourceId,
        String identifier
) {

    public TrackMetadata withId(int id) {
        return new TrackMetadata(id, trackName, author, art, trackUrl, isrc, durationMs, sourceId, identifier);
    }
}
