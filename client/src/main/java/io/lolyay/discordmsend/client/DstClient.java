package io.lolyay.discordmsend.client;

import io.lolyay.discordmsend.client.net.NetworkingClient;
import io.lolyay.discordmsend.network.Enviroment;
import io.lolyay.discordmsend.network.protocol.NetworkPhase;
import io.lolyay.discordmsend.network.protocol.packet.PacketRegistry;
import io.lolyay.discordmsend.network.protocol.packet.packets.C2S.postenc.*;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.CacheExpireS2C;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.SearchMultipleResponseS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackDetailsS2CPacket;
import io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.TrackSearchResponseS2CPacket;
import io.lolyay.discordmsend.obj.CUserData;
import io.lolyay.discordmsend.obj.MusicTrack;
import io.lolyay.discordmsend.util.logging.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Discord Music Send Client - Connects to a remote music server to control audio playback.
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // 1. Create client with user data
 * CUserData data = new CUserData("MyBot", "1.0", "Author", features);
 * Client client = new Client(data);
 * 
 * // 2. Set event handler (REQUIRED before connect)
 * client.setEventHandler(new MusicEventHandler());
 * 
 * // 3. Connect to server
 * client.connect(7, "localhost", 2677);
 * 
 * // 4. Wait for connection
 * client.waitUntilConnected();
 * 
 * // 5. Initialize with Discord user ID
 * client.setUserId(123456789L);
 * 
 * // 6. Set default volume
 * client.setDefaultVolume(50);
 * 
 * // 7. Create player for a guild
 * client.createPlayer(guildId);
 * 
 * // 8. Now you can use the client
 * client.searchTrack("song name", true).thenAccept(track -> {
 *     client.playTrack(track, guildId);
 * });
 * }</pre>
 * 
 * <h2>IMPORTANT: Method Call Order</h2>
 * <ol>
 *   <li>Constructor: {@code new Client(data)}</li>
 *   <li>{@link #setEventHandler(ClientEventHandler)}</li>
 *   <li>{@link #connect(String, int)}</li>
 *   <li>{@link #waitUntilConnected()}</li>
 *   <li>{@link #setUserId(long)}</li>
 *   <li>{@link #setDefaultVolume(int)}</li>
 *   <li>{@link #createPlayer(long)}</li>
 * </ol>
 * 
 * @see ClientEventHandler
 * @see CUserData
 */
public class DstClient {
    
    private enum ClientState {
        CREATED,           // Constructor called
        EVENT_HANDLER_SET, // setEventHandler called
        CONNECTING,        // connect() called
        CONNECTED,         // waitUntilConnected() completed
        USER_INITIALIZED,  // setUserId called
        VOLUME_SET,        // setDefaultVolume called  
        READY              // createPlayer called, fully ready
    }
    
    private ClientState state = ClientState.CREATED;
    private NetworkingClient client;
    private long pingSentTimestamp = System.currentTimeMillis();
    private float pingMs;

    private final String apiKey;

    // Track info and search futures
    private final Map<Integer, CompletableFuture<ClientTrackInfo>> trackInfoRequests = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<MusicTrack>> pendingTracks = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<List<MusicTrack>>> pendingMultipleSearches = new ConcurrentHashMap<>();
    private final Map<Integer, CompletableFuture<String>> pendingLinkRequests = new ConcurrentHashMap<>();

    // Cache
    private final Map<Integer, ClientTrackInfo> id2TrackInfoCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> queryCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> seq2Query = new ConcurrentHashMap<>();

    private final static PacketRegistry packetRegistry = new PacketRegistry();
    {
        try{
            packetRegistry.registerAll();
            Logger.debug("Registered All packets");
        } catch (Exception e){
            Logger.err("Couldn't Register Packets, ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private final Map<Long,PlayerStatus> playerStatusMap = new ConcurrentHashMap<>();
    private final Set<Long> createdPlayers = ConcurrentHashMap.newKeySet();

    private static final AtomicInteger sequence = new AtomicInteger(0);
    private final CUserData userData;
    private Thread networkThread;

    private ServerStatus status;

    private ClientEventHandler eventHandler;

    /**
     * Sets the event handler for player events. MUST be called before connect().
     * 
     * @param eventHandler Handler for player lifecycle events (pause, resume, track start, etc.)
     * @throws IllegalStateException if called after connect() or if handler is null
     */
    public void setEventHandler(ClientEventHandler eventHandler) {
        if (state != ClientState.CREATED) {
            throw new IllegalStateException("setEventHandler() must be called before connect(). Current state: " + state);
        }
        if (eventHandler == null) {
            throw new IllegalArgumentException("EventHandler cannot be null");
        }
        this.eventHandler = eventHandler;
        state = ClientState.EVENT_HANDLER_SET;
    }

    public ClientEventHandler getEventHandler() {
        return eventHandler;
    }


    /**
     * Forces a reconnect for the specified guild's player.
     *
     * @param guildId Discord guild ID
     */
    public void forceReconnect(long guildId){
        client.connection.send(new ForceReconnectC2SPacket(guildId));
    }

    @ApiStatus.Internal
    public void setPongRecieved(long timeMs){
        //TODO why dont we use the data in the ping? that would represent a more accurate ping and allow sequenced pings!
        this.pingMs = (float) (System.currentTimeMillis() - pingSentTimestamp) / 2;
    }

    public Map<Long,PlayerStatus> getPlayerStatusMap(){
        return playerStatusMap;
    }



    public void setServerStatus(ServerStatus status){
        this.status = status;
    }

    public ServerStatus getServerStatus(){
        return status;
    }

    public float getPing(){
        return pingMs;
    }

    public void setPlayerStatus(PlayerStatus status){
        playerStatusMap.put(status.getGuildId(), status);
    }

    public PlayerStatus getPlayerStatus(long guildId){
        return playerStatusMap.get(guildId);
    }

    public String getApiKey(){
        return apiKey;
    }

    public DstClient(CUserData data, String apiKey) {
        this.userData = data;
        this.apiKey = apiKey;
    }

    public CUserData getUserData() {
        return userData;
    }

    public void disconnect(String reason){
        if (client != null) {
            client.disconnect(reason);
            client = null;
        }
        state = ClientState.EVENT_HANDLER_SET; // Ready to connect
    }

    /**
     * Connects to the remote music server. Must be called after setEventHandler().
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @throws IllegalStateException if event handler not set
     */
    public void connect(String host, int port) {
        if (state != ClientState.EVENT_HANDLER_SET) {
            throw new IllegalStateException("Must call setEventHandler() before connect(). Current state: " + state);
        }
        networkThread = new Thread(() -> {
            try {
                this.client = new NetworkingClient(Enviroment.PROTOCOL_VERSION, host, port, packetRegistry, this);
                this.client.start();
                Logger.info("Client started successfully");
            } catch (Exception e) {
                Logger.err("Failed to start client", e);
                throw new RuntimeException(e);
            }
        }, "DiscordMSend-NetworkThread");
        networkThread.setDaemon(true);
        networkThread.start();
        state = ClientState.CONNECTING;
    }

    /**
     * Initializes the client with your Discord bot's user ID. Must be called after waitUntilConnected().
     * 
     * @param botId Your Discord bot's user ID
     * @throws IllegalStateException if not yet connected
     */
    public void setUserId(long botId) {
        if (state != ClientState.CONNECTED) {
            throw new IllegalStateException("Must call waitUntilConnected() before setUserId(). Current state: " + state);
        }
        client.connection.send(new DiscordDetailsC2SPacket(botId, 1));
        state = ClientState.USER_INITIALIZED;
    }

    /**
     * Creates a player for a guild. Must be called after setDefaultVolume().
     * If a player already exists for this guild, logs a warning and does nothing.
     * 
     * @param guildId Discord guild (server) ID
     * @throws IllegalStateException if initialization not complete
     */
    public void createPlayer(long guildId) {
        if (state != ClientState.VOLUME_SET && state != ClientState.READY) {
            throw new IllegalStateException("Must call setDefaultVolume() before createPlayer(). Current state: " + state);
        }
        
        if (createdPlayers.contains(guildId)) {
            Logger.warn("Player already exists for guild " + guildId + ". Ignoring duplicate createPlayer() call.");
            return;
        }
        
        client.connection.send(new PlayerCreateC2SPacket(guildId));
        createdPlayers.add(guildId);
        state = ClientState.READY;
    }
    
    /**
     * Checks if a player has been created for the specified guild.
     * 
     * @param guildId Discord guild ID
     * @return true if a player exists for this guild
     */
    public boolean hasPlayer(long guildId) {
        return createdPlayers.contains(guildId);
    }

    /**
     * Connects a player to a Discord voice server. Call after receiving Discord's VoiceServerUpdate event.
     * 
     * @param guildId Discord guild ID
     * @param sessionId Voice session ID from Discord
     * @param endpoint Voice server endpoint from Discord
     * @param token Voice server token from Discord
     */
    public void connectToServer(long guildId, String sessionId, String endpoint, String token) {
        ensureReady();
        client.connection.send(new PlayerConnectC2SPacket(guildId, endpoint, sessionId, token));
    }

    /** 
     * Requests full metadata for an existing track.
     * 
     * @param track The track to get info for
     * @return Future containing track metadata (title, author, artwork, duration)
     */
    public CompletableFuture<ClientTrackInfo> getTrackInfo(MusicTrack track) {
        ensureReady();
        int trackId = track.getTrackId();
        if (id2TrackInfoCache.containsKey(trackId)) {
            return CompletableFuture.completedFuture(id2TrackInfoCache.get(trackId));
        }

        CompletableFuture<ClientTrackInfo> future = new CompletableFuture<>();
        trackInfoRequests.put(trackId, future);
        client.connection.send(new RequestTrackInfoC2SPacket(trackId));
        return future;
    }

    /**
     * Injects a track from a URL.
     *
     * @apiNote Right now, you won't get any Track info besides the Track uri when not loading from youtube, in future releases this will be fixed.
     *
     * @param trackUri The URL of the track
     * @param isYoutubeTrack If true, the track is from YouTube; if false, it's from another source
     * @return Future containing the injected track
     */
    public CompletableFuture<MusicTrack> injectTrack(String trackUri, boolean isYoutubeTrack){
        ensureReady();
        int seq = sequence.getAndIncrement();
        CompletableFuture<MusicTrack> future = new CompletableFuture<>();
        pendingTracks.put(seq, future);
        client.connection.send(new InjectTrackC2SPacket(trackUri, seq, true, isYoutubeTrack)); // true = also get details
        return future;
    }

    /** 
     * Searches YouTube (or YouTube Music) for a track by query.
     * <p/>
     * If you want to use a URL see {@link #injectTrack}
     * 
     * @param query Search query (e.g., "Rockefeller Street")
     * @param music If true, searches YouTube Music; if false, searches regular YouTube
     * @return Future containing the found track
     */
    public CompletableFuture<MusicTrack> searchTrack(String query, boolean music) {
        ensureReady();
        if (queryCache.containsKey(query)) {
            return CompletableFuture.completedFuture(MusicTrack.ofId(queryCache.get(query)));
        }

        int seq = sequence.getAndIncrement();
        CompletableFuture<MusicTrack> future = new CompletableFuture<>();
        pendingTracks.put(seq, future);
        seq2Query.put(seq, query);

        client.connection.send(new SearchC2SPacket(query, music, seq, true)); // true = also get details
        return future;
    }

    /**
     * Searches for multiple tracks matching a query.
     * 
     * @param query Search query
     * @param music If true, searches YouTube Music
     * @param maxResults Maximum number of results to return
     * @return Future containing list of matching tracks
     */
    public CompletableFuture<List<MusicTrack>> searchTracksMultiple(String query, boolean music, int maxResults) {
        ensureReady();
        int seq = sequence.getAndIncrement();
        CompletableFuture<List<MusicTrack>> future = new CompletableFuture<>();
        pendingMultipleSearches.put(seq, future);
        client.connection.send(new SearchMultipleC2SPacket(query, music, seq, true, maxResults));

        return future;
    }

    /**
     * Requests a download link for the currently playing track in a guild.
     * The server will convert the cached PCM audio to MP3 and upload to S3.
     * 
     * @param guildId Discord guild ID
     * @return Future containing the download URL, or error message on failure
     */
    public CompletableFuture<String> requestLinkForCurrentSong(long guildId) {
        ensureReady();
        int seq = sequence.getAndIncrement();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingLinkRequests.put(seq, future);
        client.connection.send(new RequestLinkC2SPacket(guildId, seq));
        
        Logger.debug("Requested link for current song in guild " + guildId + " (seq: " + seq + ")");
        return future;
    }

    /**
     * Plays a track in the specified guild.
     * 
     * @param track Track to play (obtained from searchTrack)
     * @param guildId Discord guild ID where to play
     */
    public void playTrack(MusicTrack track, long guildId){
        ensureReady();
        client.connection.send(new PlayTrackC2SPacket(track.getTrackId(), guildId));
    }

    /**
     * Sets the default volume for all players. Must be called after setUserId().
     * 
     * @param volume Volume level (0-Technically there is no limit)
     * @throws IllegalStateException if user not initialized
     */
    public void setDefaultVolume(int volume){
        if (state != ClientState.USER_INITIALIZED) {
            throw new IllegalStateException("Must call setUserId() before setDefaultVolume(). Current state: " + state);
        }
        client.connection.send(new SetDefaultVolumeC2SPacket(volume));
        state = ClientState.VOLUME_SET;
    }

    @ApiStatus.Internal
    public void onLinkResponse(io.lolyay.discordmsend.network.protocol.packet.packets.S2C.postenc.LinkResponseS2CPacket packet) {
        CompletableFuture<String> future = pendingLinkRequests.remove(packet.sequence());
        if (future == null) {
            Logger.warn("Received link response for unknown sequence: " + packet.sequence());
            return;
        }

        if (packet.success()) {
            future.complete(packet.link());
            Logger.info("Link request successful: " + packet.link());
        } else {
            future.completeExceptionally(new RuntimeException(packet.errorMessage()));
            Logger.err("Link request failed: " + packet.errorMessage());
        }
    }


    // --- Packet handlers ---
    @ApiStatus.Internal
    public void onTrackInfo(TrackDetailsS2CPacket packet) {
        ClientTrackInfo info = new ClientTrackInfo(
                packet.titleName(),
                packet.author(),
                packet.art(),
                packet.duration(),
                packet.id()
        );

        id2TrackInfoCache.put(packet.id(), info);

        CompletableFuture<ClientTrackInfo> f = trackInfoRequests.remove(packet.id());
        if (f != null) f.complete(info);
    }

    @ApiStatus.Internal
    public void onTrackSearchResponse(TrackSearchResponseS2CPacket packet) {
        CompletableFuture<MusicTrack> future = pendingTracks.remove(packet.sequence());
        if (future == null) return;

        MusicTrack track = MusicTrack.ofId(packet.id());
        future.complete(track);

        // Cache query → trackId
        String query = seq2Query.remove(packet.sequence());
        if (query != null) queryCache.put(query, packet.id());
    }

    @ApiStatus.Internal
    public void onSearchMultipleResponse(SearchMultipleResponseS2CPacket packet) {
        CompletableFuture<List<MusicTrack>> future = pendingMultipleSearches.remove(packet.sequence());
        if (future == null) return;

        List<MusicTrack> tracks = packet.ids().stream()
                .map(MusicTrack::ofId)
                .toList();

        future.complete(tracks);

        // Optional: request details for each track (like `true` flag)
        for (MusicTrack track : tracks) {
            getTrackInfo(track).thenAccept(info -> {
                Logger.debug("Fetched details for: " + info.trackName + " by " + info.trackAuthor);
            });
        }
    }

    @ApiStatus.Internal
    public void onCacheExpire(CacheExpireS2C packet) {
        for (int trackId : packet.expiredTrackIds()) {
            id2TrackInfoCache.remove(trackId);
            Logger.debug("Removed expired track from cache: " + trackId);
        }
        
        // Also remove from query cache (reverse lookup)
        queryCache.entrySet().removeIf(entry -> packet.expiredTrackIds().contains(entry.getValue()));
    }


    /**
     * Blocks until the client is fully connected and authenticated. Must be called after connect().
     * 
     * @throws IllegalStateException if connect() not called
     */
    @Blocking
    public void waitUntilConnected(){
        if (state != ClientState.CONNECTING) {
            throw new IllegalStateException("Must call connect() before waitUntilConnected(). Current state: " + state);
        }
        while(client == null || client.connection == null || client.connection.getPhase() != NetworkPhase.POST_ENCRYPTION){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        state = ClientState.CONNECTED;
    }
    /**
     * <p>DEPRECATED: Don't use this method. The YouTube-DLP source is the only default source for now.</p>
     *
     * @deprecated since 5.0.0-P40, for removal in next major version
     * @param guildId Discord guild ID
     */
    @Deprecated(since = "5.0.0-P40", forRemoval = true)
    public void setYoutubeDlp(long guildId){
        try {
            ensureReady();
            client.connection.send(new SetSourceC2SPacket(guildId, true));
        } catch (Exception e){
            Logger.err("CLIENT: Failed to send SetSourceC2SPacket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * <p>DEPRECATED: Don't use this method. The YouTube-DLP source is the only default source for now.</p>
     *
     * @deprecated since 5.0.0-P40, for removal in next major version
     * @param guildId Discord guild ID
     */
    @Deprecated(since = "5.0.0-P40", forRemoval = true)

    public void setDefaultSource(long guildId){
        ensureReady();
        client.connection.send(new SetSourceC2SPacket(guildId, false));
    }
    // Methods

    /**
     * Sets the volume for a specific guild's player.
     * 
     * @param guildId Discord guild ID
     * @param volume Volume level (0-100)
     */
    public void setVolume(long guildId, int volume){
        ensureReady();
        client.connection.send(new PlayerSetVolumeC2SPacket(guildId, volume));
    }

    /**
     * Pauses playback in the specified guild.
     * 
     * @param guildId Discord guild ID
     */
    public void pause(long guildId){
        ensureReady();
        client.connection.send(new PlayerPauseC2SPacket(guildId));
    }

    /**
     * Resumes playback in the specified guild.
     * 
     * @param guildId Discord guild ID
     */
    public void resume(long guildId){
        ensureReady();
        client.connection.send(new PlayerResumeC2SPacket(guildId));
    }

    /**
     * Stops playback in the specified guild.
     * 
     * @param guildId Discord guild ID
     */
    public void stop(long guildId){
        ensureReady();
        client.connection.send(new PlayerStopC2SPacket(guildId));
    }

    /**
     * Sends a ping to measure latency.
     * <p>
     * Note: This method does Not get called Automatically, so the ping is going to be 0 until you call this!
     *
     */
    public void ping(){
        ensureReady();
        client.connection.send(new PingC2SPacket(System.currentTimeMillis()));
        pingSentTimestamp = System.currentTimeMillis();
        //TODO: make this ping automatically!
    }

    // --- Helpers ---

    private void ensureReady() {
        if (state != ClientState.READY) {
            throw new IllegalStateException("Client is not ready. Complete initialization first. Current state: " + state);
        }
    }
}
