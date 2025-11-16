# 🌐 DST Network Protocol Documentation

Complete documentation for developers building custom clients for the Discord Stream Toucher (DST) server.

## Table of Contents

- [Overview](#overview)
- [Connection Flow](#connection-flow)
- [Packet Structure](#packet-structure)
- [Encryption](#encryption)
- [Network Phases](#network-phases)
- [Packet Reference](#packet-reference)
- [Implementation Guide](#implementation-guide)
- [Examples](#examples)

## Overview

### Protocol Information

- **Protocol Version**: `32`
- **Default Port**: `2677`
- **Transport**: TCP (Ultra-low latency design)
- **Encoding**: Custom binary protocol with Netty + VarInt compression
- **Encryption**: Hybrid RSA-2048 + AES-128 CFB8
- **Byte Order**: Big Endian
- **Audio Quality**: Up to 510kbps Opus (3.3x better than Lavalink)
- **Latency**: <10ms packet overhead (TCP optimized for audio)

### Key Features

- **Direct Audio Streaming**: Stream Opus audio packets directly to clients
- **Real-time Position Tracking**: Frame-accurate timing updates every 100ms
- **Multi-Guild Support**: Single connection handles multiple Discord guilds
- **Hybrid YouTube Playback**: yt-source primary, yt-dlp fallback, cipher server integration
- **Intelligent Caching**: PCM track cache with automatic TTL expiration
- **FFmpeg Integration**: High-quality transcoding and format conversion

### Architecture

```
Client                          Server
  |                               |
  |---- Handshake (Plain) ------->|
  |<--- Encryption Request -------|
  |---- Encryption Response ----->|
  |                               |
  |===== Encrypted Channel =======|
  |                               |
  |---- Hello (Encrypted) ------->|
  |<--- Server Info --------------|
  |---- Discord Details --------->|
  |---- Player Commands --------->|
  |<--- Player Updates ------------|
  |<--- Track Details ------------|
```

## Connection Flow

### 1. Initial Handshake (Pre-Encryption)

**Client → Server: HandShakeC2SPacket**
```java
{
    int protocolVersion,  // Must be 31
    String host,          // Server hostname
    String apiKey,        // Authentication key
    int port              // Server port
}
```

**Server → Client: EncryptionRequestS2CPacket**
```java
{
    byte[] publicKey,     // RSA-2048 public key
    byte[] verifyToken    // 4-byte random verification token
}
```

### 2. Encryption Setup

**Client → Server: EncryptionResponseC2SPacket**
```java
{
    byte[] sharedSecret,  // AES-128 key encrypted with server's RSA public key
    byte[] verifyToken    // Verification token encrypted with same RSA key
}
```

After this exchange, **all subsequent packets are AES-encrypted**.

### 3. Post-Encryption Handshake

**Client → Server: EncHelloC2SPacket**
```java
{
    String userAgent,      // Client name/version
    String userVersion,    // Client version
    String userAuthor,     // Client author
    ClientFeatures features // Client capabilities
}
```

**Server → Client: EncHelloS2CPacket**
```java
{
    String serverName,         // Server name
    String serverVersion,      // Server version
    int port,                  // Server port
    ServerFeatures features,   // Server capabilities
    String ytSourceVersion,    // YouTube source version
    ModdedInfo moddedInfo,     // Mod information
    String countryCode         // Server country code
}
```

### 4. Discord Connection (if using Discord mode)

**Client → Server: DiscordDetailsC2SPacket**
```java
{
    long userId  // Discord user ID
}
```

## Packet Structure

### Base Packet Format

```
[Packet ID - VarInt] [Packet Data - Variable Length]
```

### Data Types

| Type | Size | Description |
|------|------|-------------|
| `byte` | 1 byte | Signed 8-bit integer |
| `boolean` | 1 byte | 0 = false, 1 = true |
| `short` | 2 bytes | Signed 16-bit integer |
| `int` | 4 bytes | Signed 32-bit integer |
| `long` | 8 bytes | Signed 64-bit integer |
| `float` | 4 bytes | IEEE 754 float |
| `double` | 8 bytes | IEEE 754 double |
| `VarInt` | 1-5 bytes | Variable-length integer |
| `String` | VarInt + UTF-8 | Length-prefixed UTF-8 string |

### VarInt Encoding

VarInts are used to save space for small integers:

```java
public static void writeVarInt(ByteBuf buf, int value) {
    while ((value & 0xFFFFFF80) != 0) {
        buf.writeByte((value & 0x7F) | 0x80);
        value >>>= 7;
    }
    buf.writeByte(value & 0x7F);
}

public static int readVarInt(ByteBuf buf) {
    int value = 0;
    int shift = 0;
    byte b;
    do {
        b = buf.readByte();
        value |= (b & 0x7F) << shift;
        shift += 7;
    } while ((b & 0x80) != 0);
    return value;
}
```

## Encryption

### Hybrid Encryption Approach

DST uses a **two-phase hybrid encryption** system for optimal security and performance:

#### Phase 1: RSA Key Exchange (Handshake Only)

1. **Server generates RSA-2048 key pair** on startup
2. **Server sends public key** in `EncryptionRequestS2CPacket`
3. **Client generates AES-128 key** (shared secret)
4. **Client encrypts**:
   - Shared secret with RSA public key
   - Verify token with RSA public key
5. **Client sends encrypted data** in `EncryptionResponseC2SPacket`
6. **Server decrypts** using RSA private key
7. **Both sides configure AES cipher** with shared secret

#### Phase 2: AES Symmetric Encryption (All Data)

After key exchange, **all packets use AES-128** for maximum performance:

- **Algorithm**: AES/CFB8/NoPadding
- **Key Size**: 128 bits (exchanged via RSA)
- **Mode**: CFB8 (Cipher Feedback 8-bit) - Stream cipher mode
- **IV**: Same as AES key (first 16 bytes)
- **Performance**: <1ms encryption overhead per packet
- **Security**: 128-bit symmetric encryption for all audio and control data

**Why Hybrid?**
- **RSA**: Secure key exchange without pre-shared secrets
- **AES**: Fast symmetric encryption for high-throughput audio streaming
- **Combined**: Security of RSA + Performance of AES

### Example Encryption Setup (Java)

```java
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;

// Generate AES key
KeyGenerator keyGen = KeyGenerator.getInstance("AES");
keyGen.init(128);
SecretKey aesKey = keyGen.generateKey();

// Encrypt with RSA
Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
byte[] encryptedSecret = rsaCipher.doFinal(aesKey.getEncoded());

// Setup AES cipher for ongoing communication
Cipher aesCipher = Cipher.getInstance("AES/CFB8/NoPadding");
IvParameterSpec iv = new IvParameterSpec(aesKey.getEncoded());
aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
```

## Network Phases

### Phase 1: PRE_ENCRYPTION

**Available Packets (Client → Server):**
- `0x00` HandShakeC2SPacket
- `0x01` EncryptionResponseC2SPacket

**Available Packets (Server → Client):**
- `0x00` EncryptionRequestS2CPacket

### Phase 2: POST_ENCRYPTION

All subsequent communication uses this phase. See [Packet Reference](#packet-reference) for complete packet list.

## Packet Reference

### Client → Server (C2S) Packets

#### Player Management

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x00` | EncHelloC2SPacket | Client identification | userAgent, userVersion, userAuthor, features |
| `0x01` | DiscordDetailsC2SPacket | Discord connection | userId |
| `0x02` | PlayerCreateC2SPacket | Create player instance | guildId |
| `0x03` | PlayerConnectC2SPacket | Connect to voice | guildId, sessionId, endPoint, token |

#### Playback Control

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x04` | PlayTrackC2SPacket | Play track | trackId, guildId |
| `0x05` | PlayerPauseC2SPacket | Pause playback | guildId |
| `0x06` | PlayerResumeC2SPacket | Resume playback | guildId |
| `0x07` | PlayerStopC2SPacket | Stop playback | guildId |
| `0x08` | PlayerSetVolumeC2SPacket | Set volume | guildId, volume |
| `0x09` | SetDefaultVolumeC2SPacket | Set default volume | volume |

#### Track Search

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x0A` | SearchC2SPacket | Search single track | query, music, sequence, details |
| `0x0B` | SearchMultipleC2SPacket | Search multiple | query, music, maxResults, sequence, details |
| `0x0C` | InjectTrackC2SPacket | Load track by URL | trackUri, details, sequence |
| `0x0D` | RequestTrackInfoC2SPacket | Get track details | trackId |

#### Utility

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x0E` | KeepAliveC2SPacket | Keep connection alive | - |
| `0x0F` | PingC2SPacket | Latency test | data |
| `0x10` | RequestLinkC2SPacket | Get shareable link | guildId, sequence |
| `0x11` | ForceReconnectC2SPacket | Force voice reconnect | guildId |
| `0x12` | SetSourceC2SPacket | *Deprecated* | guildId, sourceType |

### Server → Client (S2C) Packets

#### Server Information

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x00` | EncHelloS2CPacket | Server identification | serverName, serverVersion, port, features, ytSourceVersion, moddedInfo, countryCode |

#### Track Information

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x01` | TrackDetailsS2CPacket | Track metadata | trackId, title, author, artworkUrl, length |
| `0x02` | TrackSearchResponseS2CPacket | Single search result | trackId, sequence |
| `0x03` | SearchMultipleResponseS2CPacket | Multiple search results | trackIds, sequence |

#### Player State

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x04` | PlayerUpdateS2CPacket | Player state update | guildId, paused, volume, position, hasTrack, trackId |

#### Utility

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x05` | KeepAliveS2CPacket | Server keepalive | - |
| `0x06` | PongS2CPacket | Ping response | data |
| `0x07` | StatisticsS2CPacket | Server statistics | maxMemory, freeMemory, cpuPercent, playerCount, clientCount |
| `0x08` | LinkResponseS2CPacket | Shareable link response | guildId, sequence, success, url, error |
| `0x09` | CacheExpireS2C | Cache expiration notice | expiredTrackIds |

#### Events

| ID | Packet Name | Purpose | Fields |
|----|-------------|---------|--------|
| `0x10` | OnTrackStartEVS2CPacket | Track started | guildId, trackId |
| `0x11` | OnTrackEndEVS2CPacket | Track ended | guildId, trackId, endReason |
| `0x12` | OnTrackExceptionEVS2CPacket | Track error | guildId, trackId, exception |
| `0x13` | OnTrackStuckEVS2CPacket | Track stuck | guildId, trackId, thresholdMs |

## Implementation Guide

### Step 1: TCP Connection

```java
// Using Netty
Bootstrap bootstrap = new Bootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline()
                .addLast(new PacketDecoder(registry))
                .addLast(new PacketEncoder(registry))
                .addLast(new ConnectionHandler());
        }
    });

ChannelFuture future = bootstrap.connect(host, port).sync();
```

### Step 2: Packet Encoder/Decoder

```java
public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf out) {
        int packetId = registry.getPacketId(packet.getClass());
        PacketCodec codec = registry.getCodec(packetId);
        
        // Write packet ID as VarInt
        writeVarInt(out, packetId);
        
        // Write packet data
        codec.encode(out, packet);
    }
}

public class PacketDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 1) return;
        
        in.markReaderIndex();
        
        // Read packet ID
        int packetId = readVarInt(in);
        
        // Get codec and decode
        PacketCodec codec = registry.getCodec(packetId);
        Packet<?> packet = codec.decode(in);
        
        out.add(packet);
    }
}
```

### Step 3: Encryption Handler

```java
public class EncryptionHandler extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        ByteBuf encrypted = ctx.alloc().buffer(msg.readableBytes());
        try {
            byte[] input = new byte[msg.readableBytes()];
            msg.readBytes(input);
            
            byte[] output = encryptCipher.update(input);
            encrypted.writeBytes(output);
            
            out.add(encrypted);
        } catch (Exception e) {
            encrypted.release();
            throw new RuntimeException(e);
        }
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
        // Similar decryption logic
    }
}
```

### Step 4: Packet Handler

```java
public class PacketHandler extends SimpleChannelInboundHandler<Packet<?>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> packet) {
        // Dispatch packet to appropriate handler
        if (packet instanceof TrackDetailsS2CPacket details) {
            handleTrackDetails(details);
        } else if (packet instanceof PlayerUpdateS2CPacket update) {
            handlePlayerUpdate(update);
        }
        // ... handle other packets
    }
}
```

## Examples

### Complete Connection Example

```java
public class DSTClient {
    private Channel channel;
    private final String host;
    private final int port;
    private final String apiKey;
    private SecretKey aesKey;
    
    public void connect() throws Exception {
        Bootstrap bootstrap = new Bootstrap()
            .group(new NioEventLoopGroup())
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                            1048576, 0, 4, 0, 4))
                        .addLast("frameEncoder", new LengthFieldPrepender(4))
                        .addLast("packetDecoder", new PacketDecoder())
                        .addLast("packetEncoder", new PacketEncoder())
                        .addLast("handler", new ClientPacketHandler());
                }
            });
        
        channel = bootstrap.connect(host, port).sync().channel();
        
        // Send handshake
        sendPacket(new HandShakeC2SPacket(31, host, apiKey, port));
    }
    
    public void handleEncryptionRequest(EncryptionRequestS2CPacket packet) throws Exception {
        // Generate AES key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        aesKey = keyGen.generateKey();
        
        // Decrypt with RSA
        PublicKey publicKey = getPublicKey(packet.publicKey());
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        byte[] encryptedSecret = rsaCipher.doFinal(aesKey.getEncoded());
        byte[] encryptedToken = rsaCipher.doFinal(packet.verifyToken());
        
        // Send response
        sendPacket(new EncryptionResponseC2SPacket(encryptedSecret, encryptedToken));
        
        // Enable encryption in pipeline
        enableEncryption();
    }
    
    private void enableEncryption() throws Exception {
        Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        
        IvParameterSpec iv = new IvParameterSpec(aesKey.getEncoded());
        encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        decryptCipher.init(Cipher.DECRYPT_MODE, aesKey, iv);
        
        channel.pipeline().addBefore("frameDecoder", "decrypt", 
            new PacketDecryptor(decryptCipher));
        channel.pipeline().addBefore("frameEncoder", "encrypt", 
            new PacketEncryptor(encryptCipher));
        
        // Send post-encryption hello
        sendPacket(new EncHelloC2SPacket(
            "MyClient/1.0.0",
            "1.0.0",
            "YourName",
            new ClientFeatures(/* ... */)
        ));
    }
    
    public void playMusic(long guildId, String query) {
        // Search for track
        sendPacket(new SearchC2SPacket(query, false, 1, true));
    }
    
    // Receive search response
    public void onSearchResponse(TrackSearchResponseS2CPacket packet) {
        // Play the found track
        sendPacket(new PlayTrackC2SPacket(packet.trackId(), guildId));
    }
}
```

### Discord Integration Example

```java
public class DiscordBot extends ListenerAdapter {
    private DSTClient dstClient;
    
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (botJoinedChannel(event)) {
            AudioManager audioManager = event.getGuild().getAudioManager();
            VoiceChannel channel = audioManager.getConnectedChannel();
            
            // Get Discord voice connection details
            String sessionId = audioManager.getSessionId();
            String endpoint = getVoiceEndpoint(event.getGuild());
            String token = getVoiceToken(event.getGuild());
            
            // Create player on DST server
            dstClient.sendPacket(new PlayerCreateC2SPacket(
                event.getGuild().getIdLong()
            ));
            
            // Connect player to Discord voice
            dstClient.sendPacket(new PlayerConnectC2SPacket(
                event.getGuild().getIdLong(),
                sessionId,
                endpoint,
                token
            ));
        }
    }
    
    @SlashCommand(name = "play", description = "Play a song")
    public void onPlay(SlashCommandInteractionEvent event) {
        String query = event.getOption("song").getAsString();
        long guildId = event.getGuild().getIdLong();
        
        // Search and play
        dstClient.playMusic(guildId, query);
        
        event.reply("Searching for: " + query).queue();
    }
}
```

### Handling Player Updates

```java
public void onPlayerUpdate(PlayerUpdateS2CPacket packet) {
    long guildId = packet.guildId();
    boolean isPaused = packet.paused();
    double volume = packet.volume();
    long position = packet.position();
    boolean hasTrack = packet.hasTrack();
    int currentTrackId = packet.trackId();
    
    // Update UI
    updatePlayerUI(guildId, isPaused, position, volume);
    
    // Request track details if needed
    if (hasTrack && !trackCache.contains(currentTrackId)) {
        sendPacket(new RequestTrackInfoC2SPacket(currentTrackId));
    }
}

public void onTrackDetails(TrackDetailsS2CPacket packet) {
    Track track = new Track(
        packet.trackId(),
        packet.title(),
        packet.author(),
        packet.artworkUrl(),
        packet.length()
    );
    
    trackCache.put(track.id(), track);
    displayNowPlaying(track);
}
```

## Advanced Features

### Track Caching System

DST implements an intelligent PCM-based caching system:

#### Cache Architecture

```
Track Playback → LavaPlayer → PCM Data → Cache File
                                    ↓
                          MD5 Hash (Cache ID)
                                    ↓
                      ./cache/tracks/{hash}.pcm
```

#### Cache Management

- **Format**: Raw PCM audio (48kHz, 16-bit, stereo)
- **Naming**: MD5 hash of track URI
- **TTL**: Configurable expiration (2-7 days)
- **Expiration Events**: Server sends `CacheExpireS2C` packet when tracks expire
- **Shared**: All guilds share the same cache pool

#### Benefits

1. **Instant Replay**: Cached tracks play immediately
2. **Bandwidth Savings**: No re-downloading from source
3. **Offline Capability**: Play cached tracks without internet
4. **Upload Integration**: Cached PCM ready for S3 upload

### Upload Functionality (S3 Integration)

When clients request shareable links, the server:

#### Upload Pipeline

```
1. Check if track is cached
2. Convert PCM to MP3 using FFmpeg
3. Upload MP3 to S3 bucket
4. Store metadata (track ID → S3 URL mapping)
5. Return public URL to client
```

#### FFmpeg Transcoding

```bash
# Server executes internally
ffmpeg -f s16le -ar 48000 -ac 2 -i {cache_id}.pcm \
       -c:a libmp3lame -b:a 320k \
       {cache_id}.mp3
```

**Quality**: 320kbps MP3 for maximum compatibility

#### S3 Upload Format

- **Path**: `pub/tracks/{cache_id}.mp3`
- **Content-Type**: `audio/mpeg`
- **ACL**: Public-read
- **URL**: `{publicDownloadBucketUrl}/pub/tracks/{cache_id}.mp3`

### Direct Audio Streaming

DST can stream audio **directly to clients** bypassing Discord:

#### Streaming Architecture

```
Server                          Client
  |                               |
  |---- PlayerUpdateS2CPacket --->| (Every 100ms)
  |     (position, trackId)       |
  |                               |
  |---- AudioFramePacket -------->| (20ms Opus frames)
  |     (Opus data, sequence)     |
  |                               |
```

#### Audio Format

- **Codec**: Opus
- **Bitrate**: 96kbps (standard) to 510kbps (HQ mode)
- **Frame Duration**: 20ms
- **Sample Rate**: 48kHz
- **Channels**: Stereo

#### Why 3.3x Better Than Lavalink?

1. **Higher Bitrate**: 510kbps vs 160kbps (Lavalink default)
2. **Opus Quality 10/10**: Maximum encoder complexity
3. **High Resampling**: SINC interpolation for upsampling
4. **Zero Discord Compression**: Direct TCP streaming
5. **Configurable Buffer**: 10-second buffer in HQ mode

### Real-Time Position Tracking

DST provides **frame-accurate position tracking**:

#### Position Update Mechanism

```java
// Server sends updates every 100ms
PlayerUpdateS2CPacket {
    long guildId,
    boolean paused,
    double volume,
    long position,      // Current position in milliseconds
    boolean hasTrack,
    int trackId
}
```

#### Accuracy

- **Update Frequency**: 10 updates/second
- **Precision**: Millisecond accuracy
- **Sync**: Frame-synchronized with audio output
- **Latency Compensation**: Server compensates for network delay

### Multi-Guild Support

Single connection, multiple guilds:

#### Architecture

```
Client Connection
    └── Session
        ├── Guild 1 (Player Instance)
        ├── Guild 2 (Player Instance)
        └── Guild 3 (Player Instance)
```

#### Features

- **Isolated State**: Each guild has independent player state
- **Shared Cache**: All guilds share track cache
- **Concurrent Playback**: Multiple guilds can play simultaneously
- **Per-Guild Volume**: Independent volume control
- **Resource Efficient**: Single LavaPlayer instance

### YouTube Hybrid Approach

DST uses a **three-tier fallback** system:

#### Tier 1: LavaPlayer yt-source (Primary)

```java
YoutubeAudioSourceManager source = new YoutubeAudioSourceManager(options);
source.setRemoteCipher(cipherServerUrl, apiKey);
```

- **Fastest**: Direct YouTube API integration
- **Most Reliable**: Built into LavaPlayer
- **Cipher Integration**: Uses remote cipher server for signature decryption

#### Tier 2: Modified yt-dlp (Fallback)

When yt-source fails:
```bash
yt-dlp --format bestaudio \
       --extract-audio \
       --audio-format opus \
       --output - \
       {youtube_url} | ffmpeg -i pipe:0 -f opus pipe:1
```

**Modified Version Features**:
- Enhanced signature decryption
- Better rate limit handling
- Optimized for streaming
- Discord-compatible format output

#### Tier 3: Cipher Server (Unrestricted Access)

External service for signature decryption:
- **Repository**: [kikkia/yt-cipher](https://github.com/kikkia/yt-cipher/)
- **Purpose**: Decrypt YouTube n-signature parameter
- **Benefit**: Bypasses client-side JavaScript requirements
- **Self-Hostable**: Full control over availability

### Ultra-Low Latency TCP

DST is optimized for **minimum latency**:

#### TCP Optimizations

```java
// Netty channel configuration
bootstrap.option(ChannelOption.TCP_NODELAY, true);         // Disable Nagle
bootstrap.option(ChannelOption.SO_KEEPALIVE, true);        // Keep connection alive
bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, 
    new WriteBufferWaterMark(8 * 1024, 32 * 1024));       // Optimize buffer
```

#### Latency Breakdown

- **Network Round Trip**: 10-50ms (depends on network)
- **Packet Encoding**: <1ms
- **Encryption Overhead**: <1ms
- **Frame Processing**: <1ms
- **Total Protocol Overhead**: <10ms

**Result**: Near-native audio latency over TCP

## Best Practices

### Connection Management

1. **Keepalive**: Send `KeepAliveC2SPacket` every 30-60 seconds
2. **Reconnection**: Implement exponential backoff for reconnection attempts
3. **Timeout**: Close connection if no packets received for 90 seconds
4. **Multi-Guild**: Reuse single connection for all guilds

### Error Handling

1. **Track Exceptions**: Handle `OnTrackExceptionEVS2CPacket`
2. **Connection Loss**: Gracefully handle disconnections
3. **Invalid Packets**: Skip malformed packets, don't crash
4. **Cache Expiration**: Handle `CacheExpireS2C` to invalidate local cache

### Performance

1. **Packet Batching**: Don't send packets too frequently (max 20/second)
2. **Buffer Management**: Release Netty ByteBuf objects properly
3. **Thread Safety**: Use thread-safe collections for shared state
4. **Position Updates**: Don't request updates faster than 100ms

### Security

1. **API Key**: Never hardcode API keys, use environment variables
2. **Validation**: Validate all incoming packet data
3. **Rate Limiting**: Implement client-side rate limiting
4. **Encryption**: Always verify encryption is enabled before sending sensitive data

## Debugging

### Enable Debug Logging

```java
System.setProperty("io.netty.leakDetection.level", "ADVANCED");
LogManager.getLogger().setLevel(Level.DEBUG);
```

### Packet Inspector

```java
public class PacketLogger extends ChannelDuplexHandler {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.debug("RECV: " + msg.getClass().getSimpleName());
        ctx.fireChannelRead(msg);
    }
    
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        logger.debug("SEND: " + msg.getClass().getSimpleName());
        ctx.write(msg, promise);
    }
}
```

## Server Requirements

Before implementing a client, ensure the server has:

### Required Dependencies

#### 1. FFmpeg
The server requires FFmpeg for:
- PCM to MP3 transcoding for uploads
- Audio format conversion
- High-quality resampling

**Installation**:
```bash
# Ubuntu/Debian
sudo apt install ffmpeg

# CentOS/RHEL
sudo yum install ffmpeg

# macOS
brew install ffmpeg

# Windows
choco install ffmpeg
```

**Verify**: `ffmpeg -version`

#### 2. yt-dlp (Modified Version)

**Important**: Server requires a modified yt-dlp build.

**Download**: `[PLACEHOLDER_URL_TO_MODIFIED_YT_DLP]`

The modified version includes:
- Enhanced YouTube signature decryption
- Optimized streaming for DST
- Better error handling
- Discord-compatible output formats

**Installation**:
```bash
# Linux/macOS
wget [PLACEHOLDER_URL] -O /usr/local/bin/yt-dlp
chmod +x /usr/local/bin/yt-dlp

# Windows - Add to PATH
```

**Verify**: `yt-dlp --version`

#### 3. YouTube Cipher Server (Optional but Recommended)

For unrestricted YouTube access:

```bash
git clone https://github.com/kikkia/yt-cipher/
cd yt-cipher
docker-compose up -d
```

Configure server to use it:
```yaml
cipherServerURl: 'http://localhost:8080/'
cipherServerApiKey: 'your-api-key'
```

## Performance Characteristics

### Audio Quality Comparison

| Metric | DST (HQ Mode) | Lavalink | Improvement |
|--------|---------------|----------|-------------|
| Max Bitrate | 510kbps | 160kbps | 3.3x |
| Opus Quality | 10/10 | 5/10 | 2x |
| Resampling | SINC High | Standard | Better |
| Frame Buffer | 10s | 2s | 5x |
| Latency | <10ms | ~50ms | 5x faster |

### Resource Usage

**Standard Mode** (Multi-Guild):
- CPU: 5-10% per active guild
- RAM: 256MB base + 50MB per guild
- Network: ~100kbps per stream

**HQ Mode** (Single Guild):
- CPU: 15-25% (maximum quality)
- RAM: 512MB base + 100MB for buffers
- Network: ~500kbps per stream

### Scalability

- **Max Guilds**: Unlimited (resource-dependent)
- **Max Concurrent Streams**: 50+ per server instance
- **Connection Limit**: 1000 simultaneous clients
- **Cache Size**: Configurable (default: 10GB)

## Troubleshooting

### Common Issues

#### 1. YouTube Playback Fails

**Symptoms**: Tracks fail to load, error events

**Solutions**:
- Verify yt-dlp is installed and in PATH
- Configure YouTube OAuth token
- Use cipher server for signature decryption
- Check cipher server is accessible

#### 2. Low Audio Quality

**Symptoms**: Compressed or poor quality audio

**Solutions**:
- Enable HQ mode: `singleGuildHQ: true`
- Verify FFmpeg supports high-quality codecs
- Check Opus encoder settings
- Ensure sufficient server resources

#### 3. High Latency

**Symptoms**: Delayed position updates, laggy controls

**Solutions**:
- Check network latency (ping server)
- Verify TCP_NODELAY is enabled
- Reduce packet send frequency
- Use wired connection if possible

#### 4. Cache Issues

**Symptoms**: Tracks re-download, slow replays

**Solutions**:
- Verify cache directory is writable
- Check disk space availability
- Ensure cache TTL is configured
- Review cache expiration events

## Support

For questions or issues:
- **GitHub Issues**: [Report bugs](https://github.com/yourusername/DiscordToucher/issues)
- **Discord Server**: [Join discussion](https://discord.gg/your-invite)
- **Email**: dev@example.com

## Version History

- **Protocol 31** (Current)
  - Added cache expiration packets
  - Added link request/response with S3 integration
  - Improved player update packets (100ms frequency)
  - Added direct audio streaming support
  - Multi-guild architecture improvements
  
- **Protocol 30**
  - Initial public release
  - Basic encryption and playback
  - Single-guild support

## Additional Resources

- **LavaPlayer Documentation**: https://github.com/sedmelluq/lavaplayer
- **Netty Guide**: https://netty.io/wiki/
- **yt-cipher Repository**: https://github.com/kikkia/yt-cipher/
- **Modified yt-dlp**: `[PLACEHOLDER_URL_TO_MODIFIED_YT_DLP]`
- **FFmpeg Documentation**: https://ffmpeg.org/documentation.html
- **Opus Codec**: https://opus-codec.org/

---

**Note**: Replace `[PLACEHOLDER_URL_TO_MODIFIED_YT_DLP]` with actual download link for the modified yt-dlp version.

Happy coding! 🚀
