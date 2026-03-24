package io.lolyay.discordmsend.obj;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AudioCodec {
    CUSTOM(null, 0, (byte) 0),
    OPUS_MAX(AudioCodecRaw.OPUS, 48000, (byte) 2),
    PCM_MAX(AudioCodecRaw.PCM, 48000, (byte) 2),
    OGG_MAX(AudioCodecRaw.VORBIS, 48000, (byte) 2);


    private final AudioCodecRaw raw;
    private final int sampleRate;
    private final byte channels;
}
