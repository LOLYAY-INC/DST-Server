package io.lolyay.discordmsend.obj;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AudioCodec {
    OPUS_MAX(AudioCodecRaw.OPUS),
    PCM_MAX(AudioCodecRaw.PCM);
    @Getter
    private final AudioCodecRaw raw;
}
