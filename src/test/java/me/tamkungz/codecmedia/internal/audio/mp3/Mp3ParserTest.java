package me.tamkungz.codecmedia.internal.audio.mp3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import me.tamkungz.codecmedia.internal.audio.BitrateMode;

class Mp3ParserTest {

    @Test
    void shouldParseSimpleMpeg1Layer3FrameSequenceAsCbr() throws Exception {
        byte[] frameHeader = new byte[] {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00};
        int frameLength = 417; // 128 kbps @ 44100, MPEG1 L3 no padding
        byte[] frame = new byte[frameLength];
        System.arraycopy(frameHeader, 0, frame, 0, frameHeader.length);

        byte[] data = new byte[frameLength * 3];
        System.arraycopy(frame, 0, data, 0, frameLength);
        System.arraycopy(frame, 0, data, frameLength, frameLength);
        System.arraycopy(frame, 0, data, frameLength * 2, frameLength);

        Mp3ProbeInfo info = Mp3Parser.parse(data);

        assertEquals("mp3", info.codec());
        assertEquals(44100, info.sampleRate());
        assertEquals(2, info.channels());
        assertEquals(BitrateMode.CBR, info.bitrateMode());
    }
}

