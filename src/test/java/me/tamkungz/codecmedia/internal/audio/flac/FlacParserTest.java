package me.tamkungz.codecmedia.internal.audio.flac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import me.tamkungz.codecmedia.CodecMediaException;
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

    @Test
    void shouldRejectReservedMetadataBlockType127() {
        byte[] flac = createFlacWithReservedMetadataType();

        CodecMediaException ex = assertThrows(CodecMediaException.class, () -> FlacParser.parse(flac));
        assertEquals("Invalid FLAC metadata block type: 127 is reserved", ex.getMessage());
    }

    @Test
    void shouldEstimateBitrateFromAudioFramesOnlyExcludingMetadata() throws Exception {
        int sampleRate = 44100;
        byte[] flac = createFlacWithLargePictureMetadata(sampleRate, 2, 16, sampleRate, 50_000, 10_000);

        FlacProbeInfo info = FlacParser.parse(flac);

        assertEquals(80, info.bitrateKbps()); // 10_000 bytes audio payload over 1 second
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

    private static byte[] createFlacWithReservedMetadataType() {
        byte[] bytes = new byte[4 + 4 + 34 + 4];
        bytes[0] = 'f'; bytes[1] = 'L'; bytes[2] = 'a'; bytes[3] = 'C';

        // STREAMINFO (not last)
        bytes[4] = 0x00;
        bytes[5] = 0x00;
        bytes[6] = 0x00;
        bytes[7] = 34;

        int streamInfoOffset = 8;
        long packed = ((long) 44100 & 0xFFFFFL) << 44;
        packed |= ((long) (2 - 1) & 0x7L) << 41;
        packed |= ((long) (16 - 1) & 0x1FL) << 36;
        packed |= (44100L & 0xFFFFFFFFFL);
        for (int i = 0; i < 8; i++) {
            bytes[streamInfoOffset + 10 + i] = (byte) ((packed >>> (56 - (i * 8))) & 0xFF);
        }

        int o = 4 + 4 + 34;
        // Reserved block type 127 + last flag
        bytes[o] = (byte) 0xFF;
        bytes[o + 1] = 0;
        bytes[o + 2] = 0;
        bytes[o + 3] = 0;
        return bytes;
    }

    private static byte[] createFlacWithLargePictureMetadata(
            int sampleRate,
            int channels,
            int bitsPerSample,
            long totalSamples,
            int pictureBytes,
            int audioBytes
    ) {
        int total = 4 + (4 + 34) + (4 + pictureBytes) + audioBytes;
        byte[] bytes = new byte[total];
        bytes[0] = 'f'; bytes[1] = 'L'; bytes[2] = 'a'; bytes[3] = 'C';

        // STREAMINFO (not last)
        int o = 4;
        bytes[o] = 0x00;
        bytes[o + 1] = 0x00;
        bytes[o + 2] = 0x00;
        bytes[o + 3] = 34;

        int streamInfoOffset = o + 4;
        long packed = ((long) sampleRate & 0xFFFFFL) << 44;
        packed |= ((long) (channels - 1) & 0x7L) << 41;
        packed |= ((long) (bitsPerSample - 1) & 0x1FL) << 36;
        packed |= (totalSamples & 0xFFFFFFFFFL);
        for (int i = 0; i < 8; i++) {
            bytes[streamInfoOffset + 10 + i] = (byte) ((packed >>> (56 - (i * 8))) & 0xFF);
        }

        // PICTURE metadata (last)
        o = streamInfoOffset + 34;
        bytes[o] = (byte) 0x86; // last=1, blockType=6 (PICTURE)
        bytes[o + 1] = (byte) ((pictureBytes >>> 16) & 0xFF);
        bytes[o + 2] = (byte) ((pictureBytes >>> 8) & 0xFF);
        bytes[o + 3] = (byte) (pictureBytes & 0xFF);

        // trailing bytes represent encoded audio frames region
        return bytes;
    }
}

