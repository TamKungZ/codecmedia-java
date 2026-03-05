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

    @Test
    void shouldDetectVbrWhenFrameBitratesDiffer() throws Exception {
        byte[] frame128 = createFrame(new byte[] {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00}, 417);
        byte[] frame64 = createFrame(new byte[] {(byte) 0xFF, (byte) 0xFB, (byte) 0x50, 0x00}, 208);

        byte[] data = new byte[frame128.length + frame64.length];
        System.arraycopy(frame128, 0, data, 0, frame128.length);
        System.arraycopy(frame64, 0, data, frame128.length, frame64.length);

        Mp3ProbeInfo info = Mp3Parser.parse(data);

        assertEquals(44100, info.sampleRate());
        assertEquals(BitrateMode.VBR, info.bitrateMode());
    }

    @Test
    void shouldParseMonoFromChannelMode() throws Exception {
        byte[] frame = createFrame(new byte[] {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, (byte) 0xC0}, 417);
        byte[] data = new byte[frame.length * 2];
        System.arraycopy(frame, 0, data, 0, frame.length);
        System.arraycopy(frame, 0, data, frame.length, frame.length);

        Mp3ProbeInfo info = Mp3Parser.parse(data);

        assertEquals(1, info.channels());
        assertEquals(BitrateMode.CBR, info.bitrateMode());
    }

    private static byte[] createFrame(byte[] header, int frameLength) {
        byte[] frame = new byte[frameLength];
        System.arraycopy(header, 0, frame, 0, header.length);
        return frame;
    }
}

