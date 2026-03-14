package me.tamkungz.codecmedia.internal.audio.flac;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.BitrateMode;

public final class FlacParser {

    private FlacParser() {
    }

    public static FlacProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (!isLikelyFlac(bytes)) {
            throw new CodecMediaException("Not a FLAC file");
        }

        int offset = 4; // skip fLaC marker
        boolean streamInfoFound = false;
        int sampleRate = 0;
        int channels = 0;
        int bitsPerSample = 0;
        long totalSamples = 0;
        int audioStartOffset = -1;

        while (offset + 4 <= bytes.length) {
            int header = bytes[offset] & 0xFF;
            boolean last = (header & 0x80) != 0;
            int blockType = header & 0x7F;
            if (blockType == 0x7F) {
                throw new CodecMediaException("Invalid FLAC metadata block type: 127 is reserved");
            }
            int length = ((bytes[offset + 1] & 0xFF) << 16)
                    | ((bytes[offset + 2] & 0xFF) << 8)
                    | (bytes[offset + 3] & 0xFF);
            offset += 4;

            if (offset + length > bytes.length) {
                throw new CodecMediaException("Invalid FLAC metadata block length");
            }

            if (blockType == 0) { // STREAMINFO
                if (length < 34) {
                    throw new CodecMediaException("Invalid FLAC STREAMINFO block");
                }
                long packed = readUInt64BE(bytes, offset + 10);
                sampleRate = (int) ((packed >>> 44) & 0xFFFFF);
                channels = (int) (((packed >>> 41) & 0x7) + 1);
                bitsPerSample = (int) (((packed >>> 36) & 0x1F) + 1);
                totalSamples = packed & 0xFFFFFFFFFL;
                streamInfoFound = true;
            }

            offset += length;
            if (last) {
                audioStartOffset = offset;
                break;
            }
        }

        if (!streamInfoFound || sampleRate <= 0 || channels <= 0 || bitsPerSample <= 0) {
            throw new CodecMediaException("FLAC STREAMINFO is missing or invalid");
        }

        long durationMillis = totalSamples > 0 ? (totalSamples * 1000L) / sampleRate : 0;
        long encodedAudioBytes = (audioStartOffset >= 0 && audioStartOffset <= bytes.length)
                ? (bytes.length - (long) audioStartOffset)
                : 0;
        int avgBitrateKbps = durationMillis > 0
                ? (int) ((encodedAudioBytes * 8L * 1000L) / durationMillis / 1000L)
                : 0;
        int pcmEquivalentKbps = (int) (((long) sampleRate * channels * bitsPerSample) / 1000L);
        int bitrateKbps = avgBitrateKbps > 0 ? avgBitrateKbps : pcmEquivalentKbps;

        return new FlacProbeInfo("flac", sampleRate, channels, bitsPerSample, bitrateKbps, BitrateMode.VBR, durationMillis);
    }

    public static boolean isLikelyFlac(byte[] bytes) {
        return bytes != null
                && bytes.length >= 4
                && bytes[0] == 'f'
                && bytes[1] == 'L'
                && bytes[2] == 'a'
                && bytes[3] == 'C';
    }

    private static long readUInt64BE(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 8 > bytes.length) {
            throw new CodecMediaException("Unexpected end of FLAC data");
        }
        return ((long) (bytes[offset] & 0xFF) << 56)
                | ((long) (bytes[offset + 1] & 0xFF) << 48)
                | ((long) (bytes[offset + 2] & 0xFF) << 40)
                | ((long) (bytes[offset + 3] & 0xFF) << 32)
                | ((long) (bytes[offset + 4] & 0xFF) << 24)
                | ((long) (bytes[offset + 5] & 0xFF) << 16)
                | ((long) (bytes[offset + 6] & 0xFF) << 8)
                | ((long) (bytes[offset + 7] & 0xFF));
    }
}

