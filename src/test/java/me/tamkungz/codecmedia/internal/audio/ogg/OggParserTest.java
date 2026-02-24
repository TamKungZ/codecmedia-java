package me.tamkungz.codecmedia.internal.audio.ogg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import me.tamkungz.codecmedia.internal.audio.BitrateMode;

class OggParserTest {

    @Test
    void shouldParseVorbisIdentificationFromFirstOggPage() throws Exception {
        byte[] data = createMinimalVorbisOgg();

        OggProbeInfo info = OggParser.parse(data);

        assertEquals("vorbis", info.codec());
        assertEquals(2, info.channels());
        assertEquals(44100, info.sampleRate());
        assertEquals(BitrateMode.UNKNOWN, info.bitrateMode());
    }

    private static byte[] createMinimalVorbisOgg() {
        int payloadSize = 30;
        byte[] data = new byte[27 + 1 + payloadSize];

        data[0] = 'O';
        data[1] = 'g';
        data[2] = 'g';
        data[3] = 'S';
        data[4] = 0; // version
        data[5] = 2; // BOS

        long granule = 0;
        for (int i = 0; i < 8; i++) {
            data[6 + i] = (byte) ((granule >>> (8 * i)) & 0xFF);
        }

        int serial = 1;
        data[14] = (byte) (serial & 0xFF);

        data[26] = 1; // segment count
        data[27] = (byte) payloadSize;

        int p = 28;
        data[p] = 0x01;
        data[p + 1] = 'v';
        data[p + 2] = 'o';
        data[p + 3] = 'r';
        data[p + 4] = 'b';
        data[p + 5] = 'i';
        data[p + 6] = 's';
        data[p + 7] = 0;
        data[p + 8] = 0;
        data[p + 9] = 0;
        data[p + 10] = 0; // vorbis version LE
        data[p + 11] = 2; // channels

        int sampleRate = 44100;
        data[p + 12] = (byte) (sampleRate & 0xFF);
        data[p + 13] = (byte) ((sampleRate >>> 8) & 0xFF);
        data[p + 14] = (byte) ((sampleRate >>> 16) & 0xFF);
        data[p + 15] = (byte) ((sampleRate >>> 24) & 0xFF);

        int nominalBitrate = 128000;
        data[p + 20] = (byte) (nominalBitrate & 0xFF);
        data[p + 21] = (byte) ((nominalBitrate >>> 8) & 0xFF);
        data[p + 22] = (byte) ((nominalBitrate >>> 16) & 0xFF);
        data[p + 23] = (byte) ((nominalBitrate >>> 24) & 0xFF);

        data[p + 28] = (byte) 0xB0;
        data[p + 29] = 1;

        return data;
    }
}

