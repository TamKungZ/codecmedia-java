package me.tamkungz.codecmedia.internal.video.mov;

import java.nio.charset.StandardCharsets;

import me.tamkungz.codecmedia.CodecMediaException;

public final class MovParser {

    private MovParser() {
    }

    public static boolean isLikelyMov(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return false;
        }
        if (!isAscii(bytes, 4, "ftyp")) {
            return false;
        }
        String major = readAscii(bytes, 8, 4);
        return "qt  ".equals(major);
    }

    public static MovProbeInfo parse(byte[] bytes) throws CodecMediaException {
        if (bytes == null || bytes.length < 12) {
            throw new CodecMediaException("MOV data is empty or too short");
        }
        if (!isAscii(bytes, 4, "ftyp")) {
            throw new CodecMediaException("Not a QuickTime/ISO-BMFF container (missing ftyp)");
        }

        String majorBrand = readAscii(bytes, 8, 4).trim();
        Integer width = null;
        Integer height = null;
        Long durationMillis = null;
        Double frameRate = null;
        String videoCodec = null;
        String audioCodec = null;
        Integer sampleRate = null;
        Integer channels = null;
        Long currentTrackTimescale = null;

        int offset = 0;
        while (offset + 8 <= bytes.length) {
            long boxSize = readUInt32(bytes, offset);
            String boxType = readAscii(bytes, offset + 4, 4);

            int headerSize = 8;
            if (boxSize == 1) {
                if (offset + 16 > bytes.length) {
                    throw new CodecMediaException("Invalid MOV extended box header");
                }
                boxSize = readUInt64(bytes, offset + 8);
                headerSize = 16;
            } else if (boxSize == 0) {
                boxSize = bytes.length - offset;
            }

            if (boxSize < headerSize) {
                throw new CodecMediaException("Invalid MOV box size for box: " + boxType);
            }

            int payloadStart = offset + headerSize;
            long payloadSizeLong = boxSize - headerSize;
            if (payloadSizeLong < 0 || payloadStart + payloadSizeLong > bytes.length) {
                throw new CodecMediaException("MOV box exceeds file bounds: " + boxType);
            }
            int payloadSize = (int) payloadSizeLong;

            if ("mvhd".equals(boxType) && durationMillis == null) {
                durationMillis = parseMvhdDuration(bytes, payloadStart, payloadSize);
            } else if ("mdhd".equals(boxType)) {
                Long parsedTimescale = parseMdhdTimescale(bytes, payloadStart, payloadSize);
                if (parsedTimescale != null && parsedTimescale > 0) {
                    currentTrackTimescale = parsedTimescale;
                }
            } else if ("tkhd".equals(boxType) && (width == null || height == null)) {
                int[] wh = parseTkhdDimensions(bytes, payloadStart, payloadSize);
                if (wh[0] > 0 && wh[1] > 0) {
                    width = wh[0];
                    height = wh[1];
                }
            } else if ("stsd".equals(boxType)) {
                SampleDescription info = parseStsd(bytes, payloadStart, payloadSize);
                if (info.videoCodec != null && videoCodec == null) {
                    videoCodec = info.videoCodec;
                }
                if (info.audioCodec != null && audioCodec == null) {
                    audioCodec = info.audioCodec;
                }
                if (info.sampleRate != null && sampleRate == null) {
                    sampleRate = info.sampleRate;
                }
                if (info.channels != null && channels == null) {
                    channels = info.channels;
                }
            } else if ("stts".equals(boxType) && frameRate == null) {
                frameRate = parseFrameRateFromStts(bytes, payloadStart, payloadSize, currentTrackTimescale);
            }

            offset += (int) boxSize;
        }

        return new MovProbeInfo(
                durationMillis,
                width,
                height,
                majorBrand,
                videoCodec,
                audioCodec,
                sampleRate,
                channels,
                frameRate
        );
    }

    private static Long parseMvhdDuration(byte[] bytes, int offset, int size) throws CodecMediaException {
        if (size < 24) {
            return null;
        }
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
        if (size < 84) {
            return new int[] {0, 0};
        }
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
        return new int[] {widthFixed >>> 16, heightFixed >>> 16};
    }

    private static Long parseMdhdTimescale(byte[] bytes, int offset, int size) throws CodecMediaException {
        if (size < 16) {
            return null;
        }
        int version = bytes[offset] & 0xFF;
        if (version == 0) {
            if (size < 20) {
                return null;
            }
            long timescale = readUInt32(bytes, offset + 12);
            return timescale > 0 ? timescale : null;
        }
        if (version == 1) {
            if (size < 32) {
                return null;
            }
            long timescale = readUInt32(bytes, offset + 20);
            return timescale > 0 ? timescale : null;
        }
        return null;
    }

    private static SampleDescription parseStsd(byte[] bytes, int offset, int size) throws CodecMediaException {
        if (size < 16) {
            return new SampleDescription();
        }

        int entryCount = (int) readUInt32(bytes, offset + 4);
        int cursor = offset + 8;
        int limit = offset + size;
        SampleDescription out = new SampleDescription();

        for (int i = 0; i < entryCount && cursor + 8 <= limit; i++) {
            long entrySizeLong = readUInt32(bytes, cursor);
            String format = readAscii(bytes, cursor + 4, 4).trim();
            if (entrySizeLong < 8) {
                break;
            }
            int entrySize = (int) entrySizeLong;
            if (cursor + entrySize > limit) {
                break;
            }

            if (isVideoFourCc(format) && out.videoCodec == null) {
                out.videoCodec = normalizeCodec(format);
            } else if (isAudioFourCc(format) && out.audioCodec == null) {
                out.audioCodec = normalizeCodec(format);
                if (entrySize >= 36) {
                    int channelCountOffset = cursor + 16;
                    int sampleRateOffset = cursor + 24;
                    if (sampleRateOffset + 4 <= cursor + entrySize) {
                        out.channels = readUInt16(bytes, channelCountOffset);
                        int srFixed = (int) readUInt32(bytes, sampleRateOffset);
                        out.sampleRate = srFixed >>> 16;
                    }
                }
            }
            cursor += entrySize;
        }

        return out;
    }

    private static Double parseFrameRateFromStts(byte[] bytes, int offset, int size, Long trackTimescale) throws CodecMediaException {
        if (size < 16) {
            return null;
        }
        if (trackTimescale == null || trackTimescale <= 0) {
            return null;
        }
        int entryCount = (int) readUInt32(bytes, offset + 4);
        if (entryCount <= 0) {
            return null;
        }

        int cursor = offset + 8;
        if (cursor + 8 > offset + size) {
            return null;
        }

        long sampleCount = readUInt32(bytes, cursor);
        long sampleDelta = readUInt32(bytes, cursor + 4);
        if (sampleCount <= 0 || sampleDelta <= 0) {
            return null;
        }
        return trackTimescale.doubleValue() / sampleDelta;
    }

    private static boolean isVideoFourCc(String fourcc) {
        return "avc1".equals(fourcc)
                || "hvc1".equals(fourcc)
                || "hev1".equals(fourcc)
                || "vp09".equals(fourcc)
                || "av01".equals(fourcc)
                || "jpeg".equals(fourcc)
                || "mjpa".equals(fourcc);
    }

    private static boolean isAudioFourCc(String fourcc) {
        return "mp4a".equals(fourcc)
                || "alac".equals(fourcc)
                || "lpcm".equals(fourcc)
                || "sowt".equals(fourcc)
                || "ulaw".equals(fourcc)
                || "twos".equals(fourcc);
    }

    private static String normalizeCodec(String fourcc) {
        return switch (fourcc) {
            case "avc1" -> "h264";
            case "hvc1", "hev1" -> "hevc";
            case "vp09" -> "vp9";
            case "av01" -> "av1";
            case "mp4a" -> "aac";
            case "lpcm", "sowt", "twos" -> "pcm";
            default -> fourcc;
        };
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
        return new String(bytes, offset, len, StandardCharsets.US_ASCII);
    }

    private static int readUInt16(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 2 > bytes.length) {
            throw new CodecMediaException("Unexpected end of MOV data");
        }
        return ((bytes[offset] & 0xFF) << 8)
                | (bytes[offset + 1] & 0xFF);
    }

    private static long readUInt32(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 4 > bytes.length) {
            throw new CodecMediaException("Unexpected end of MOV data");
        }
        return ((long) (bytes[offset] & 0xFF) << 24)
                | ((long) (bytes[offset + 1] & 0xFF) << 16)
                | ((long) (bytes[offset + 2] & 0xFF) << 8)
                | ((long) (bytes[offset + 3] & 0xFF));
    }

    private static long readUInt64(byte[] bytes, int offset) throws CodecMediaException {
        if (offset + 8 > bytes.length) {
            throw new CodecMediaException("Unexpected end of MOV data");
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

    private static final class SampleDescription {
        String videoCodec;
        String audioCodec;
        Integer sampleRate;
        Integer channels;
    }
}

