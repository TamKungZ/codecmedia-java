package me.tamkungz.codecmedia.internal.video.mp4;

import me.tamkungz.codecmedia.CodecMediaException;

public final class Mp4Parser {

    private Mp4Parser() {
    }

    public static boolean isLikelyMp4(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        if (!isAscii(bytes, 4, "ftyp")) {
            return false;
        }
        String major = readAscii(bytes, 8, 4);
        return "isom".equals(major)
                || "iso2".equals(major)
                || "avc1".equals(major)
                || "mp41".equals(major)
                || "mp42".equals(major)
                || "qt  ".equals(major);
    }

    public static Mp4ProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (!isLikelyMp4(bytes)) {
            throw new CodecMediaException("Not an MP4/ISO BMFF file");
        }

        String majorBrand = readAscii(bytes, 8, 4);
        Integer width = null;
        Integer height = null;
        Long durationMillis = null;

        int offset = 0;
        while (offset + 8 <= bytes.length) {
            long boxSize = readUInt32(bytes, offset);
            String boxType = readAscii(bytes, offset + 4, 4);
            if (boxSize == 0) {
                boxSize = bytes.length - offset;
            } else if (boxSize == 1) {
                if (offset + 16 > bytes.length) {
                    throw new CodecMediaException("Invalid extended MP4 box size");
                }
                boxSize = readUInt64(bytes, offset + 8);
            }
            if (boxSize < 8) {
                throw new CodecMediaException("Invalid MP4 box size for box: " + boxType);
            }

            int headerSize = (readUInt32(bytes, offset) == 1) ? 16 : 8;
            int payloadStart = offset + headerSize;
            long payloadSizeLong = boxSize - headerSize;
            if (payloadSizeLong < 0 || payloadStart + payloadSizeLong > bytes.length) {
                throw new CodecMediaException("MP4 box exceeds file bounds: " + boxType);
            }

            int payloadSize = (int) payloadSizeLong;
            if ("mvhd".equals(boxType) && payloadSize >= 20 && durationMillis == null) {
                durationMillis = parseMvhdDuration(bytes, payloadStart, payloadSize);
            } else if ("tkhd".equals(boxType) && payloadSize >= 84 && (width == null || height == null)) {
                int[] wh = parseTkhdDimensions(bytes, payloadStart, payloadSize);
                if (wh[0] > 0 && wh[1] > 0) {
                    width = wh[0];
                    height = wh[1];
                }
            }

            offset += (int) boxSize;
        }

        return new Mp4ProbeInfo(durationMillis, width, height, majorBrand.trim());
    }

    private static Long parseMvhdDuration(byte[] bytes, int offset, int size) throws CodecMediaException {
        int version = bytes[offset] & 0xFF;
        if (version == 0) {
            if (size < 20) {
                return null;
            }
            long timescale = readUInt32(bytes, offset + 12);
            long duration = readUInt32(bytes, offset + 16);
            if (timescale <= 0 || duration <= 0) {
                return null;
            }
            return (duration * 1000L) / timescale;
        }
        if (version == 1) {
            if (size < 32) {
                return null;
            }
            long timescale = readUInt32(bytes, offset + 20);
            long duration = readUInt64(bytes, offset + 24);
            if (timescale <= 0 || duration <= 0) {
                return null;
            }
            return (duration * 1000L) / timescale;
        }
        return null;
    }

    private static int[] parseTkhdDimensions(byte[] bytes, int offset, int size) throws CodecMediaException {
        int version = bytes[offset] & 0xFF;
        int widthOffset;
        int heightOffset;
        if (version == 0) {
            widthOffset = offset + 76;
            heightOffset = offset + 80;
        } else {
            widthOffset = offset + 88;
            heightOffset = offset + 92;
        }
        if (widthOffset + 4 > offset + size || heightOffset + 4 > offset + size) {
            return new int[] {0, 0};
        }

        int widthFixed = (int) readUInt32(bytes, widthOffset);
        int heightFixed = (int) readUInt32(bytes, heightOffset);
        return new int[] { widthFixed >>> 16, heightFixed >>> 16 };
    }

    private static boolean isAscii(byte[] bytes, int offset, String literal) {
        if (offset + literal.length() > bytes.length) {
            return false;
        }
        for (int i = 0; i < literal.length(); i++) {
            if ((char) bytes[offset + i] != literal.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static String readAscii(byte[] bytes, int offset, int len) {
        if (offset + len > bytes.length) {
            return "";
        }
        return new String(bytes, offset, len, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static long readUInt32(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 4 > bytes.length) {
            throw new CodecMediaException("Unexpected end of MP4 data");
        }
        return ((long) (bytes[offset] & 0xFF) << 24)
                | ((long) (bytes[offset + 1] & 0xFF) << 16)
                | ((long) (bytes[offset + 2] & 0xFF) << 8)
                | ((long) (bytes[offset + 3] & 0xFF));
    }

    private static long readUInt64(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 8 > bytes.length) {
            throw new CodecMediaException("Unexpected end of MP4 data");
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

