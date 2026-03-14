package me.tamkungz.codecmedia.internal.audio.wav;

import java.nio.charset.StandardCharsets;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.BitrateMode;

public final class WavParser {

    private static final int WAVE_FORMAT_PCM = 0x0001;
    private static final int WAVE_FORMAT_IEEE_FLOAT = 0x0003;
    private static final int WAVE_FORMAT_EXTENSIBLE = 0xFFFE;

    private WavParser() {
    }

    public static WavProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (!isLikelyWav(bytes)) {
            throw new CodecMediaException("Not a WAV/RIFF file");
        }

        boolean isRf64 = bytes[0] == 'R' && bytes[1] == 'F' && bytes[2] == '6' && bytes[3] == '4';
        int offset = 12;
        Integer audioFormat = null;
        Integer channels = null;
        Integer sampleRate = null;
        Long avgByteRate = null;
        Integer bitsPerSample = null;
        Long dataSize = null;
        Long ds64DataSize = null;

        while (offset + 8 <= bytes.length) {
            String chunkId = new String(bytes, offset, 4, StandardCharsets.US_ASCII);
            long chunkSize = readLeUInt32(bytes, offset + 4);

            int chunkDataStart = offset + 8;
            long chunkDataEnd = chunkDataStart + chunkSize;
            if (chunkDataEnd < chunkDataStart || chunkDataEnd > bytes.length) {
                if (!(isRf64 && "data".equals(chunkId) && chunkSize == 0xFFFFFFFFL && ds64DataSize != null)) {
                    throw new CodecMediaException("WAV chunk exceeds file bounds: " + chunkId);
                }
            }

            if ("ds64".equals(chunkId)) {
                if (chunkSize < 16) {
                    throw new CodecMediaException("Invalid RF64 ds64 chunk");
                }
                long parsedDataSize = readLeLong(bytes, chunkDataStart + 8);
                if (parsedDataSize < 0) {
                    throw new CodecMediaException("RF64 data size is too large");
                }
                ds64DataSize = parsedDataSize;
            } else if ("fmt ".equals(chunkId)) {
                if (chunkSize < 16) {
                    throw new CodecMediaException("WAV fmt chunk is too small");
                }
                audioFormat = readLeShort(bytes, chunkDataStart);
                channels = readLeShort(bytes, chunkDataStart + 2);
                sampleRate = readLeInt(bytes, chunkDataStart + 4);
                avgByteRate = readLeUInt32(bytes, chunkDataStart + 8);
                bitsPerSample = readLeShort(bytes, chunkDataStart + 14);

                validateSupportedAudioFormat(audioFormat, bytes, chunkDataStart, chunkSize);
            } else if ("data".equals(chunkId)) {
                if (chunkSize == 0xFFFFFFFFL && isRf64) {
                    if (ds64DataSize == null) {
                        throw new CodecMediaException("RF64 data chunk uses 0xFFFFFFFF size but ds64 is missing");
                    }
                    dataSize = ds64DataSize;
                } else {
                    dataSize = chunkSize;
                }
            }

            long effectiveChunkSize = chunkSize;
            if (isRf64 && "data".equals(chunkId) && chunkSize == 0xFFFFFFFFL) {
                long available = bytes.length - chunkDataStart;
                long expected = ds64DataSize != null ? ds64DataSize : available;
                if (expected < 0 || expected > available) {
                    throw new CodecMediaException("RF64 data chunk exceeds file bounds");
                }
                effectiveChunkSize = expected;
            }

            long padded = (effectiveChunkSize % 2 == 0) ? effectiveChunkSize : effectiveChunkSize + 1;
            long nextOffset = chunkDataStart + padded;
            if (nextOffset < chunkDataStart || nextOffset > bytes.length || nextOffset > Integer.MAX_VALUE) {
                throw new CodecMediaException("WAV chunk exceeds file bounds: " + chunkId);
            }
            offset = (int) nextOffset;
        }

        if (audioFormat == null || channels == null || sampleRate == null || bitsPerSample == null || dataSize == null) {
            throw new CodecMediaException("WAV is missing required fmt/data chunks");
        }
        if (channels <= 0 || sampleRate <= 0 || bitsPerSample <= 0) {
            throw new CodecMediaException("Invalid WAV format values");
        }

        long computedByteRate = (long) sampleRate * channels * bitsPerSample / 8L;
        long byteRate = (avgByteRate != null && avgByteRate > 0) ? avgByteRate : computedByteRate;
        if (byteRate <= 0) {
            throw new CodecMediaException("Invalid WAV byte rate");
        }

        long durationMillis = (dataSize * 1000L) / byteRate;
        int bitrateKbps = (int) ((byteRate * 8L) / 1000L);
        return new WavProbeInfo(durationMillis, bitrateKbps, sampleRate, channels, BitrateMode.CBR);
    }

    public static boolean isLikelyWav(byte[] bytes) {
        return bytes.length >= 12
                && ((bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F')
                || (bytes[0] == 'R' && bytes[1] == 'F' && bytes[2] == '6' && bytes[3] == '4'))
                && bytes[8] == 'W'
                && bytes[9] == 'A'
                && bytes[10] == 'V'
                && bytes[11] == 'E';
    }

    private static void validateSupportedAudioFormat(int audioFormat, byte[] bytes, int fmtOffset, long fmtChunkSize)
            throws CodecMediaException {
        if (audioFormat == WAVE_FORMAT_PCM || audioFormat == WAVE_FORMAT_IEEE_FLOAT) {
            return;
        }
        if (audioFormat != WAVE_FORMAT_EXTENSIBLE) {
            throw new CodecMediaException("Unsupported WAV audio format: 0x" + Integer.toHexString(audioFormat));
        }

        if (fmtChunkSize < 40) {
            throw new CodecMediaException("Invalid WAV extensible fmt chunk");
        }
        int cbSize = readLeShort(bytes, fmtOffset + 16);
        if (cbSize < 22) {
            throw new CodecMediaException("Invalid WAV extensible fmt extension size");
        }

        int subFormatOffset = fmtOffset + 24;
        int subType = readLeShort(bytes, subFormatOffset);
        boolean validGuid = bytes[subFormatOffset + 2] == 0
                && bytes[subFormatOffset + 3] == 0
                && bytes[subFormatOffset + 4] == 0x10
                && bytes[subFormatOffset + 5] == 0
                && (bytes[subFormatOffset + 6] & 0xFF) == 0x80
                && bytes[subFormatOffset + 7] == 0
                && bytes[subFormatOffset + 8] == 0
                && (bytes[subFormatOffset + 9] & 0xFF) == 0xAA
                && bytes[subFormatOffset + 10] == 0
                && (bytes[subFormatOffset + 11] & 0xFF) == 0x38
                && (bytes[subFormatOffset + 12] & 0xFF) == 0x9B
                && bytes[subFormatOffset + 13] == 0x71;
        if (!validGuid || (subType != WAVE_FORMAT_PCM && subType != WAVE_FORMAT_IEEE_FLOAT)) {
            throw new CodecMediaException("Unsupported WAV extensible sub-format: 0x" + Integer.toHexString(subType));
        }
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

    private static long readLeUInt32(byte[] bytes, int offset) throws CodecMediaException {
        return readLeInt(bytes, offset) & 0xFFFFFFFFL;
    }

    private static long readLeLong(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 8 > bytes.length) {
            throw new CodecMediaException("Unexpected end of WAV data");
        }
        return (bytes[offset] & 0xFFL)
                | ((bytes[offset + 1] & 0xFFL) << 8)
                | ((bytes[offset + 2] & 0xFFL) << 16)
                | ((bytes[offset + 3] & 0xFFL) << 24)
                | ((bytes[offset + 4] & 0xFFL) << 32)
                | ((bytes[offset + 5] & 0xFFL) << 40)
                | ((bytes[offset + 6] & 0xFFL) << 48)
                | ((bytes[offset + 7] & 0xFFL) << 56);
    }
}

