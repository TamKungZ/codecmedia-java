package me.tamkungz.codecmedia.internal.audio.wav;

import java.nio.charset.StandardCharsets;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.BitrateMode;

public final class WavParser {

    private WavParser() {
    }

    public static WavProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (!isLikelyWav(bytes)) {
            throw new CodecMediaException("Not a WAV/RIFF file");
        }

        int offset = 12;
        Integer channels = null;
        Integer sampleRate = null;
        Integer bitsPerSample = null;
        Long dataSize = null;

        while (offset + 8 <= bytes.length) {
            String chunkId = new String(bytes, offset, 4, StandardCharsets.US_ASCII);
            int chunkSize = readLeInt(bytes, offset + 4);
            if (chunkSize < 0) {
                throw new CodecMediaException("Invalid WAV chunk size: " + chunkSize);
            }

            int chunkDataStart = offset + 8;
            if (chunkDataStart + chunkSize > bytes.length) {
                throw new CodecMediaException("WAV chunk exceeds file bounds: " + chunkId);
            }

            if ("fmt ".equals(chunkId)) {
                if (chunkSize < 16) {
                    throw new CodecMediaException("WAV fmt chunk is too small");
                }
                channels = readLeShort(bytes, chunkDataStart + 2);
                sampleRate = readLeInt(bytes, chunkDataStart + 4);
                bitsPerSample = readLeShort(bytes, chunkDataStart + 14);
            } else if ("data".equals(chunkId)) {
                dataSize = (long) chunkSize;
            }

            int padded = (chunkSize % 2 == 0) ? chunkSize : chunkSize + 1;
            offset = chunkDataStart + padded;
        }

        if (channels == null || sampleRate == null || bitsPerSample == null || dataSize == null) {
            throw new CodecMediaException("WAV is missing required fmt/data chunks");
        }
        if (channels <= 0 || sampleRate <= 0 || bitsPerSample <= 0) {
            throw new CodecMediaException("Invalid WAV format values");
        }

        long byteRate = (long) sampleRate * channels * bitsPerSample / 8L;
        if (byteRate <= 0) {
            throw new CodecMediaException("Invalid WAV byte rate");
        }

        long durationMillis = (dataSize * 1000L) / byteRate;
        int bitrateKbps = (int) ((byteRate * 8L) / 1000L);
        return new WavProbeInfo(durationMillis, bitrateKbps, sampleRate, channels, BitrateMode.CBR);
    }

    public static boolean isLikelyWav(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'A'
                && bytes[10] == 'V'
                && bytes[11] == 'E';
    }

    private static int readLeShort(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 2 > bytes.length) {
            throw new CodecMediaException("Unexpected end of WAV data");
        }
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static int readLeInt(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 4 > bytes.length) {
            throw new CodecMediaException("Unexpected end of WAV data");
        }
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }
}

