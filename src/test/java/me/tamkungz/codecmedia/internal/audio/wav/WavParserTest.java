package me.tamkungz.codecmedia.internal.audio.wav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import me.tamkungz.codecmedia.internal.audio.BitrateMode;

class WavParserTest {

    @Test
    void shouldParsePcm16Stereo44100() throws Exception {
        byte[] wav = createPcmWav(2, 44100, 16, 44100 * 2 * 2);

        WavProbeInfo info = WavParser.parse(wav);

        assertEquals(2, info.channels());
        assertEquals(44100, info.sampleRate());
        assertEquals(1411, info.bitrateKbps());
        assertEquals(1000, info.durationMillis());
        assertEquals(BitrateMode.CBR, info.bitrateMode());
    }

    @Test
    void shouldParsePcm8Mono22050() throws Exception {
        byte[] wav = createPcmWav(1, 22050, 8, 22050);

        WavProbeInfo info = WavParser.parse(wav);

        assertEquals(1, info.channels());
        assertEquals(22050, info.sampleRate());
        assertEquals(176, info.bitrateKbps());
        assertEquals(1000, info.durationMillis());
        assertEquals(BitrateMode.CBR, info.bitrateMode());
    }

    private static byte[] createPcmWav(int channels, int sampleRate, int bitsPerSample, int dataSize) {
        int chunkSize = 36 + dataSize;
        byte[] out = new byte[44 + dataSize];

        out[0] = 'R'; out[1] = 'I'; out[2] = 'F'; out[3] = 'F';
        writeLeInt(out, 4, chunkSize);
        out[8] = 'W'; out[9] = 'A'; out[10] = 'V'; out[11] = 'E';

        out[12] = 'f'; out[13] = 'm'; out[14] = 't'; out[15] = ' ';
        writeLeInt(out, 16, 16);
        writeLeShort(out, 20, 1);
        writeLeShort(out, 22, channels);
        writeLeInt(out, 24, sampleRate);
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        writeLeInt(out, 28, byteRate);
        int blockAlign = channels * bitsPerSample / 8;
        writeLeShort(out, 32, blockAlign);
        writeLeShort(out, 34, bitsPerSample);

        out[36] = 'd'; out[37] = 'a'; out[38] = 't'; out[39] = 'a';
        writeLeInt(out, 40, dataSize);
        return out;
    }

    private static void writeLeShort(byte[] out, int offset, int value) {
        out[offset] = (byte) (value & 0xFF);
        out[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    }

    private static void writeLeInt(byte[] out, int offset, int value) {
        out[offset] = (byte) (value & 0xFF);
        out[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        out[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        out[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }
}
