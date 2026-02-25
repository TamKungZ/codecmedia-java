package me.tamkungz.codecmedia.internal.image.webp;

import me.tamkungz.codecmedia.CodecMediaException;

public final class WebpParser {

    private WebpParser() {
    }

    public static boolean isLikelyWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    public static WebpProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (!isLikelyWebp(bytes)) {
            throw new CodecMediaException("Not a WebP file");
        }
        if (bytes.length < 30) {
            throw new CodecMediaException("WebP is too small");
        }

        String chunkType = fourcc(bytes, 12);
        return switch (chunkType) {
            case "VP8 " -> parseVp8(bytes);
            case "VP8L" -> parseVp8L(bytes);
            case "VP8X" -> parseVp8X(bytes);
            default -> throw new CodecMediaException("Unsupported WebP chunk type: " + chunkType);
        };
    }

    private static WebpProbeInfo parseVp8X(byte[] bytes) throws CodecMediaException {
        if (bytes.length < 30) {
            throw new CodecMediaException("Invalid VP8X chunk length");
        }
        int widthMinus1 = (bytes[24] & 0xFF) | ((bytes[25] & 0xFF) << 8) | ((bytes[26] & 0xFF) << 16);
        int heightMinus1 = (bytes[27] & 0xFF) | ((bytes[28] & 0xFF) << 8) | ((bytes[29] & 0xFF) << 16);
        return ensurePositive(widthMinus1 + 1, heightMinus1 + 1, 8, "VP8X");
    }

    private static WebpProbeInfo parseVp8L(byte[] bytes) throws CodecMediaException {
        if (bytes.length < 25) {
            throw new CodecMediaException("Invalid VP8L chunk length");
        }
        if ((bytes[20] & 0xFF) != 0x2F) {
            throw new CodecMediaException("Invalid VP8L signature byte");
        }
        int b1 = bytes[21] & 0xFF;
        int b2 = bytes[22] & 0xFF;
        int b3 = bytes[23] & 0xFF;
        int b4 = bytes[24] & 0xFF;
        int widthMinus1 = b1 | ((b2 & 0x3F) << 8);
        int heightMinus1 = ((b2 >> 6) & 0x03) | (b3 << 2) | ((b4 & 0x0F) << 10);
        return ensurePositive(widthMinus1 + 1, heightMinus1 + 1, 8, "VP8L");
    }

    private static WebpProbeInfo parseVp8(byte[] bytes) throws CodecMediaException {
        if (bytes.length < 30) {
            throw new CodecMediaException("Invalid VP8 chunk length");
        }
        if ((bytes[23] & 0xFF) != 0x9D || (bytes[24] & 0xFF) != 0x01 || (bytes[25] & 0xFF) != 0x2A) {
            throw new CodecMediaException("Invalid VP8 frame start code");
        }
        int width = ((bytes[27] & 0x3F) << 8) | (bytes[26] & 0xFF);
        int height = ((bytes[29] & 0x3F) << 8) | (bytes[28] & 0xFF);
        return ensurePositive(width, height, 8, "VP8");
    }

    private static String fourcc(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 4 > bytes.length) {
            throw new CodecMediaException("Unexpected end of WebP data");
        }
        return new String(bytes, offset, 4, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static WebpProbeInfo ensurePositive(int width, int height, Integer bitDepth, String variant) throws CodecMediaException {
        if (width <= 0 || height <= 0) {
            throw new CodecMediaException("WebP " + variant + " has invalid dimensions");
        }
        return new WebpProbeInfo(width, height, bitDepth);
    }
}

