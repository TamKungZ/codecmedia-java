package me.tamkungz.codecmedia.internal.audio.flac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import me.tamkungz.codecmedia.internal.audio.BitrateMode;

class FlacParserTest {

    @Test
    void shouldParseStreamInfoFromMinimalFlac() throws Exception {
        byte[] flac = createMinimalFlac(44100, 2, 16, 44100);

        FlacProbeInfo info = FlacParser.parse(flac);

        assertEquals("flac", info.codec());
        assertEquals(44100, info.sampleRate());
        assertEquals(2, info.channels());
        assertEquals(16, info.bitsPerSample());
        assertEquals(BitrateMode.VBR, info.bitrateMode());
        assertEquals(1000, info.durationMillis());
        assertEquals(1411, info.bitrateKbps());
    }

    private static byte[] createMinimalFlac(int sampleRate, int channels, int bitsPerSample, long totalSamples) {
        byte[] bytes = new byte[4 + 4 + 34];
        bytes[0] = 'f'; bytes[1] = 'L'; bytes[2] = 'a'; bytes[3] = 'C';

        // Last-metadata-block flag + STREAMINFO type
        bytes[4] = (byte) 0x80;
        bytes[5] = 0x00;
        bytes[6] = 0x00;
        bytes[7] = 34;

        int streamInfoOffset = 8;
        // min/max block size + min/max frame size left as zeroes for minimal test

        long packed = ((long) sampleRate & 0xFFFFFL) << 44;
        packed |= ((long) (channels - 1) & 0x7L) << 41;
        packed |= ((long) (bitsPerSample - 1) & 0x1FL) << 36;
        packed |= (totalSamples & 0xFFFFFFFFFL);

        for (int i = 0; i < 8; i++) {
            bytes[streamInfoOffset + 10 + i] = (byte) ((packed >>> (56 - (i * 8))) & 0xFF);
        }

        return bytes;
    }
}

