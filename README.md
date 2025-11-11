# 🎵 Discord Stream Toucher (DST)

[![Version](https://img.shields.io/badge/version-5.0.3-blue.svg)](https://github.com/yourusername/DiscordToucher)
[![Protocol](https://img.shields.io/badge/protocol-31-green.svg)](https://github.com/yourusername/DiscordToucher)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-brightgreen.svg)](LICENSE)

A high-performance music streaming server for Discord with end-to-end encryption, multi-source support, and a flexible client-server architecture.

## ✨ Features

- 🎶 **Multi-Source Audio Streaming**
  - YouTube (hybrid yt-source + yt-dlp approach)
  - YouTube Music
  - SoundCloud
  - Bandcamp
  - Twitch Streams
  - Yandex Music
  - Direct HTTP URLs
  - And more...

- 🔒 **Security First**
  - Hybrid encryption: RSA-2048 key exchange + AES-128 for data
  - End-to-end encrypted audio streaming
  - API key authentication
  - Secure packet protocol with VarInt compression

- ⚡ **Ultra-High Performance**
  - **3.3x better quality than Lavalink** - Maximum fidelity audio
  - **Ultra-low latency TCP streaming** - Direct audio packets to client
  - Netty-based networking with zero-copy transfers
  - LavaPlayer + FFmpeg audio engine
  - FFmpeg-powered transcoding for maximum compatibility
  - Configurable quality settings (up to Opus 10/10)
  - Advanced track caching system with TTL management

- 🎛️ **Advanced Features**
  - **Multi-guild support** - Single server, multiple Discord servers
  - **Stream audio directly to client** - Bypass Discord, stream to any client
  - **Real-time track timing updates** - Precise position tracking
  - Track search and playback control
  - Volume management and track seeking
  - S3 upload integration for permanent track sharing
  - Server statistics monitoring
  - YouTube cipher server integration for unrestricted access

## 📦 Installation

### Prerequisites

- Java 17 or higher
- **FFmpeg** - Required for audio transcoding and format conversion
- **yt-dlp** (Modified version) - Required for YouTube playback
- A Discord bot token (if using Discord mode)
- (Optional) S3-compatible storage for track sharing
- (Optional) YouTube OAuth refresh token for unrestricted access

### Installing FFmpeg

#### Windows
```bash
# Using Chocolatey
choco install ffmpeg

# Or download from https://www.gyan.dev/ffmpeg/builds/
# Extract and add to the same folder as DST-Server
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install ffmpeg
```

#### Linux (CentOS/RHEL)
```bash
sudo yum install epel-release
sudo yum install ffmpeg
```

#### macOS
```bash
brew install ffmpeg
```

Verify installation:
```bash
ffmpeg -version
```

### Installing yt-dlp (Modified Version)

**Important**: DST requires a modified version of yt-dlp with enhanced YouTube support.

#### Download Modified yt-dlp
https://github.com/YAYLOLDEV/yt-dlp2/releases/latest

#### Why Modified yt-dlp?

The modified version includes:
- Enhanced YouTube signature decryption
- Better rate limit handling
- Improved compatibility with Discord streaming
- Optimized for use with the DST server

Verify installation:
```bash
yt-dlp --version
```

### Download from Releases

1. Go to the [Releases](https://github.com/yourusername/DiscordToucher/releases) page
2. Download the latest `Dst-Server.jar`
3. Place it in a dedicated directory

### First Run

```bash
java -jar Dst-Server.jar
```

The server will:
1. Create a `config.yml` with default settings
2. Exit automatically

## ⚙️ Configuration

Edit the generated `config.yml`:

```yaml
# ========================================
# DCT Server Configuration
# ========================================

# Enable caching of tracks
enableTrackCache: true

# Allow users to download tracks
allowDownload: true

# Single guild HQ mode - enables maximum quality settings (use for 1 server only)
singleGuildHQ: false

# Enable debug logging
debug: false

# ========================================
# S3 Upload Configuration
# ========================================

# S3 bucket URL for uploads (e.g., https://eu2.contabostorage.com/bucket-name)
trackUploadBucketUrl: ''

# Public S3 URL for downloads (e.g., https://files.example.com/hash:bucket)
publicDownloadBucketUrl: ''

# S3 access key (leave empty to disable upload functionality)
s3AccessKey: ''

# S3 secret key
s3SecretKey: ''

# S3 region code (e.g., eu-central-1, us-east-1)
s3Region: eu-central-1

# ========================================
# YouTube Configuration
# ========================================

# URL of the YouTube cipher server
cipherServerURl: ''

# API key for cipher server
cipherServerApiKey: ''

# YouTube OAuth refresh token
youtubeOauthRefreshToken: ''

# ========================================
# General Settings
# ========================================

# Default country code for operations
countryCode: US

# API key for client authentication
apiKey: your-secure-api-key-here
```

### Configuration Options Explained

#### General Settings

- **enableTrackCache**: Enable local track caching (recommended)
- **allowDownload**: Allow clients to request track downloads
- **singleGuildHQ**: Enable maximum quality mode (recommended for single Discord server)
- **debug**: Enable verbose debug logging

#### S3 Upload Configuration

Configure these if you want to share tracks via S3:

- **trackUploadBucketUrl**: Your S3 bucket endpoint and name
- **publicDownloadBucketUrl**: Public URL for accessing uploaded tracks
- **s3AccessKey** & **s3SecretKey**: S3 credentials
- **s3Region**: AWS region code

Leave empty to disable upload functionality.

#### YouTube Configuration

- **cipherServerURl**: Custom cipher server (leave empty for default)
- **cipherServerApiKey**: Cipher server API key (leave empty for default)
- **youtubeOauthRefreshToken**: OAuth token for bypassing YouTube restrictions

#### API Key

Set a secure API key in the config. Clients must provide this key to connect.

## 🚀 Running the Server

### Standard Mode (Multi-Guild)

```bash
java -jar Dst-Server.jar
```

- Supports multiple Discord servers
- Balanced resource usage
- 2-day track cache
- Standard audio quality

### High-Quality Mode (Single Guild)

Set `singleGuildHQ: true` in config.yml:

```bash
java -jar Dst-Server.jar
```

- Maximum audio quality (Opus 10/10)
- 10-second buffer for ultra-smooth playback
- 7-day track cache
- 6-8 concurrent threads
- Recommended for single Discord server deployment

### Production Deployment

#### Using systemd (Linux)

Create `/etc/systemd/system/dst-server.service`:

```ini
[Unit]
Description=Discord Stream Transformer Server
After=network.target

[Service]
Type=simple
User=dst
WorkingDirectory=/opt/dst
ExecStart=/usr/bin/java -Xmx2G -Xms1G -jar Dst-Server.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl enable dst-server
sudo systemctl start dst-server
sudo systemctl status dst-server
```

#### Using Docker

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY Dst-Server.jar .
COPY config.yml .

EXPOSE 2677

CMD ["java", "-Xmx2G", "-Xms1G", "-jar", "Dst-Server.jar"]
```

Build and run:

```bash
docker build -t dst-server .
docker run -d \
  --name dst-server \
  -p 2677:2677 \
  -v ./config.yml:/app/config.yml \
  -v ./cache:/app/cache \
  --restart unless-stopped \
  dst-server
```

## 🎯 Core Features Explained

### Track Caching System

DST implements an intelligent track caching system:

- **PCM Cache**: Raw audio data cached locally for instant playback
- **TTL Management**: Automatic expiration based on last access
  - Standard mode: 2 days
  - HQ mode: 7 days
- **Upload Integration**: Cached tracks can be uploaded to S3 for permanent storage
- **Cache Expiration Events**: Clients notified when tracks expire

The cache significantly improves:
- Replay performance (instant playback)
- Server resource usage (no re-downloading)
- User experience (no buffering on replays)

### Upload Functionality

When a track finishes playing, it can be uploaded to S3:

1. **Server caches PCM audio** during playback
2. **Client requests shareable link** via `RequestLinkC2SPacket`
3. **Server converts PCM to MP3** using FFmpeg
4. **Server uploads to S3** asynchronously
5. **Server returns public URL** to client

This allows:
- Permanent track storage
- Sharing tracks outside Discord
- Building playlists with guaranteed availability
- Archiving favorite music

### Direct Audio Streaming

DST can stream audio **directly to clients** via TCP packets:

- **Ultra-low latency** - No Discord intermediary
- **High fidelity** - Full Opus quality (up to 510kbps)
- **Real-time position updates** - Frame-accurate tracking
- **No Discord limitations** - Stream to any device

Use cases:
- Desktop music player integration
- Web-based streaming interfaces
- Mobile app development
- Custom audio pipelines

### Multi-Guild Architecture

Single DST server supports multiple Discord guilds simultaneously:

- **One server instance** handles all guilds
- **Isolated player instances** per guild
- **Shared track cache** across guilds
- **Independent volume and state** per guild
- **Efficient resource usage** - Shared LavaPlayer backend

Perfect for:
- Bot hosting services
- Community bot setups
- Multi-server Discord bots
- Development testing

## 🎮 Using the Built-in Client

The project includes a Java client for controlling the server.

### Building the Client

```bash
mvn clean package -pl client -am
```

The client JAR will be in `client/target/Dst-Client.jar`

### Connecting to Server

```java
import io.lolyay.discordmsend.client.ServerConnection;

// Connect to server
ServerConnection connection = new ServerConnection(
    "localhost",     // Server host
    2677,           // Server port
    "your-api-key"  // API key from config
);

connection.connect();

// Wait for connection
Thread.sleep(1000);

// Send Discord connection details (if using Discord mode)
connection.sendDiscordDetails(yourDiscordUserId);
```

### Playing Music

```java
// Create a player for a Discord guild
connection.createPlayer(guildId);

// Connect to Discord voice channel
connection.connectPlayer(guildId, sessionId, endpoint, token);

// Search for a track
connection.search("never gonna give you up", false, (trackId) -> {
    // Play the found track
    connection.playTrack(trackId, guildId);
});

// Control playback
connection.pausePlayer(guildId);
connection.resumePlayer(guildId);
connection.setVolume(guildId, 50); // 0-100
connection.stopPlayer(guildId);
```

### Advanced Features

```java
// Search multiple tracks
connection.searchMultiple("query", false, 10, (trackIds) -> {
    // Handle multiple results
});

// Get track details
connection.requestTrackInfo(trackId);

// Inject track by URL
connection.injectTrack("https://youtube.com/watch?v=...", true);

// Request shareable link (requires S3 configuration)
connection.requestLink(guildId);

// Force reconnect (if audio breaks)
connection.forceReconnect(guildId);
```

### Handling Events

```java
connection.setListener(new ServerPostEncryptionPacketListener() {
    @Override
    public void onPlayerUpdate(PlayerUpdateS2CPacket packet) {
        System.out.println("Player state: paused=" + packet.paused() 
            + ", position=" + packet.position());
    }
    
    @Override
    public void onTrackDetails(TrackDetailsS2CPacket packet) {
        System.out.println("Track: " + packet.title() 
            + " by " + packet.author());
    }
    
    @Override
    public void onLinkResponse(LinkResponseS2CPacket packet) {
        if (packet.success()) {
            System.out.println("Download link: " + packet.url());
        }
    }
    
    @Override
    public void onStatistics(StatisticsS2CPacket packet) {
        System.out.println("Server stats: " 
            + packet.playerCount() + " players, " 
            + packet.clientCount() + " clients");
    }
});
```

## 📡 Server Endpoints

- **Default Port**: `2677`
- **Protocol**: Custom TCP with RSA+AES encryption
- **Protocol Version**: `31`

## 🔧 Troubleshooting

### Server won't start

- **Check Java version**: Must be Java 17+
  ```bash
  java -version
  ```
- **Check port availability**: Ensure port 2677 is free
  ```bash
  netstat -an | grep 2677
  ```
- **Check logs**: Enable debug mode in config.yml

### YouTube playback issues

- **Configure OAuth token**: Add `youtubeOauthRefreshToken` to config
- **Check cipher server**: Verify `cipherServerURl` is accessible
- **Update dependencies**: Ensure you're using the latest release

### Audio quality issues

- **Enable HQ mode**: Set `singleGuildHQ: true` (single server only)
- **Check network**: Ensure stable connection to Discord
- **Increase buffer**: HQ mode uses larger buffers automatically

### Connection refused

- **Verify API key**: Must match between client and server config
- **Check firewall**: Allow port 2677 through firewall
- **Verify address**: Ensure client connects to correct IP/hostname

## 🏗️ Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/DiscordToucher.git
cd DiscordToucher

# Build all modules
mvn clean package

# Output locations:
# - Server: server/target/Dst-Server.jar
# - Client: client/target/Dst-Client.jar
# - Shared: shared/target/Dst-Shared.jar
```

## 📚 Documentation

- [Network Protocol Documentation](NETWORK_DOCS.md) - For building custom clients
- [Configuration Guide](CONFIG_GUIDE.md) - Detailed configuration options
- [API Reference](API_REFERENCE.md) - Complete packet reference

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [LavaPlayer](https://github.com/sedmelluq/lavaplayer) - Audio player library
- [Netty](https://netty.io/) - Network application framework
- [KOE](https://github.com/KyokoBot/koe) - Discord voice library
- [Configurate](https://github.com/SpongePowered/Configurate) - Configuration library

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/DiscordToucher/issues)
- **Discord**: [Join our Discord](https://discord.gg/your-invite)
- **Email**: support@example.com

## 🗺️ Roadmap

- [ ] Web-based admin panel
- [ ] Plugin system for custom sources
- [ ] Redis support for distributed caching
- [ ] WebSocket API for browser clients
- [ ] Prometheus metrics endpoint
- [ ] Docker Compose setup with Redis

---

Made with ❤️ by the DST Team
