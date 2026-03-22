package io.lolyay.discordmsend.server.music.providers;

import io.lolyay.discordmsend.network.types.TrackMetadata;

import java.util.List;

public interface ISearcher {
    boolean canSearch(String query);
    List<TrackMetadata> search(String query, int limit);
}
