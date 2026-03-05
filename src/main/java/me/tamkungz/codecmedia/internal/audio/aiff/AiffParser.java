package me.tamkungz.codecmedia.internal.audio.aiff;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.BitrateMode;

public final class AiffParser {

    private AiffParser() {
    }

    public static AiffProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (!isLikelyAiff(bytes)) {
            throw new CodecMediaException("Not an AIFF file");
        }

        int offset = 12;
        Integer channels = null;
        Integer bitsPerSample = null;
        Integer sampleRate = null;
        Long frameCount = null;

        while (offset + 8 <= bytes.length) {
            String chunkId = readAscii(bytes, offset, 4);
            int chunkSize = readBeInt(bytes, offset + 4);
            if (chunkSize < 0) {
                throw new CodecMediaException("Invalid AIFF chunk size: " + chunkSize);
            }

            int chunkDataStart = offset + 8;
            if (chunkDataStart + chunkSize > bytes.length) {
                throw new CodecMediaException("AIFF chunk exceeds file bounds: " + chunkId);
            }

            if ("COMM".equals(chunkId)) {
                if (chunkSize < 18) {
                    throw new CodecMediaException("AIFF COMM chunk too small");
                }
                channels = readBeShort(bytes, chunkDataStart);
                frameCount = readBeUInt32(bytes, chunkDataStart + 2);
                bitsPerSample = readBeShort(bytes, chunkDataStart + 6);
                sampleRate = decodeExtended80ToIntHz(bytes, chunkDataStart + 8);
            }

            int padded = (chunkSize % 2 == 0) ? chunkSize : chunkSize + 1;
            offset = chunkDataStart + padded;
        }

        if (channels == null || bitsPerSample == null || sampleRate == null || frameCount == null) {
            throw new CodecMediaException("AIFF missing required COMM chunk fields");
        }
        if (channels <= 0 || bitsPerSample <= 0 || sampleRate <= 0 || frameCount < 0) {
            throw new CodecMediaException("Invalid AIFF format values");
        }

        long durationMillis = (frameCount * 1000L) / sampleRate;
        long byteRate = (long) sampleRate * channels * bitsPerSample / 8L;
        int bitrateKbps = (int) ((byteRate * 8L) / 1000L);

        return new AiffProbeInfo(durationMillis, bitrateKbps, sampleRate, channels, BitrateMode.CBR);
    }

    public static boolean isLikelyAiff(byte[] bytes) {
        return bytes != null
                && bytes.length >= 12
                && bytes[0] == 'F'
                && bytes[1] == 'O'
                && bytes[2] == 'R'
                && bytes[3] == 'M'
                && bytes[8] == 'A'
                && bytes[9] == 'I'
                && bytes[10] == 'F'
                && (bytes[11] == 'F' || bytes[11] == 'C');
    }

    private static int decodeExtended80ToIntHz(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 10 > bytes.length) {
            throw new CodecMediaException("Unexpected end of AIFF data");
        }

        int exp = ((bytes[offset] & 0x7F) << 8) | (bytes[offset + 1] & 0xFF);
        long mantissa = 0;
        for (int i = 0; i < 8; i++) {
            mantissa = (mantissa << 8) | (bytes[offset + 2 + i] & 0xFFL);
        }

        if (exp == 0 || mantissa == 0) {
            return 0;
        }

        int shift = exp - 16383 - 63;
        long value;
        if (shift >= 0) {
            value = mantissa << Math.min(shift, 30);
        } else {
            value = mantissa >>> Math.min(-shift, 63);
        }

        if (value <= 0 || value > Integer.MAX_VALUE) {
            throw new CodecMediaException("Unsupported AIFF sample rate encoding");
        }
        return (int) value;
    }

    private static String readAscii(byte[] bytes, int offset, int len) throws CodecMediaException {
        if (offset + len > bytes.length) {
            throw new CodecMediaException("Unexpected end of AIFF data");
        }
        return new String(bytes, offset, len, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static int readBeShort(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 2 > bytes.length) {
            throw new CodecMediaException("Unexpected end of AIFF data");
        }
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private static int readBeInt(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 4 > bytes.length) {
            throw new CodecMediaException("Unexpected end of AIFF data");
        }
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static long readBeUInt32(byte[] bytes, int offset) throws CodecMediaException {
        return readBeInt(bytes, offset) & 0xFFFFFFFFL;
    }
}

