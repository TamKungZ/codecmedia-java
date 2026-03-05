package me.tamkungz.codecmedia.internal.audio.ogg;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.BitrateMode;
import me.tamkungz.codecmedia.internal.io.ByteArrayReader;

public final class OggParser {

    private static final int OPUS_GRANULE_RATE = 48_000;

    private OggParser() {
    }

    public static OggProbeInfo parse(byte[] data) throws CodecMediaException {
        if (data == null || data.length < 27) {
            throw new CodecMediaException("Invalid OGG data: too small");
        }

        OggPageHeader firstPage = parsePageHeader(data, 0);
        if (firstPage == null) {
            throw new CodecMediaException("Invalid OGG stream: missing OggS header");
        }

        int identOffset = firstPage.headerSize();
        int firstPayloadSize = firstPage.payloadSize();
        if (identOffset + firstPayloadSize > data.length || firstPayloadSize <= 0) {
            throw new CodecMediaException("Invalid OGG stream: incomplete first packet payload");
        }

        AudioIdent ident = parseIdentificationPacket(data, identOffset, firstPayloadSize);

        long payloadBits = 0;
        int pageCount = 0;
        long maxGranule = 0;
        int offset = 0;
        while (offset + 27 <= data.length) {
            OggPageHeader page = parsePageHeader(data, offset);
            if (page == null) {
                break;
            }
            payloadBits += (long) page.payloadSize() * 8;
            pageCount++;
            if (page.granulePosition() > maxGranule) {
                maxGranule = page.granulePosition();
            }
            offset += page.totalPageSize();
        }

        int granuleRate = ident.granuleRate() > 0 ? ident.granuleRate() : ident.sampleRate();
        long durationMillis = (granuleRate > 0 && maxGranule > 0)
                ? (maxGranule * 1000L) / granuleRate
                : 0;

        int avgBitrate = durationMillis > 0
                ? (int) ((payloadBits * 1000L) / durationMillis / 1000L)
                : 0;

        int nominalKbps = ident.nominalBitrate() > 0 ? (int) (ident.nominalBitrate() / 1000L) : 0;
        int bitrateKbps = avgBitrate > 0 ? avgBitrate : nominalKbps;

        BitrateMode mode = switch (ident.codec()) {
            case "vorbis" -> (ident.nominalBitrate() > 0 || pageCount > 2) ? BitrateMode.VBR : BitrateMode.UNKNOWN;
            case "opus" -> BitrateMode.VBR;
            default -> BitrateMode.UNKNOWN;
        };

        return new OggProbeInfo(ident.codec(), ident.sampleRate(), ident.channels(), bitrateKbps, mode, durationMillis);
    }

    private static AudioIdent parseIdentificationPacket(byte[] data, int identOffset, int payloadSize) throws CodecMediaException {
        if (isVorbisIdentification(data, identOffset, payloadSize)) {
            if (payloadSize < 30) {
                throw new CodecMediaException("Invalid OGG Vorbis stream: incomplete identification packet");
            }
            ByteArrayReader ident = new ByteArrayReader(data);
            ident.position(identOffset + 7);
            ident.readU32LE(); // vorbis version
            int channels = ident.readU8();
            int sampleRate = (int) ident.readU32LE();
            long bitrateNominal = ident.readU32LE();
            return new AudioIdent("vorbis", sampleRate, channels, bitrateNominal, sampleRate);
        }

        if (isOpusIdentification(data, identOffset, payloadSize)) {
            if (payloadSize < 19) {
                throw new CodecMediaException("Invalid OGG Opus stream: incomplete OpusHead packet");
            }
            ByteArrayReader ident = new ByteArrayReader(data);
            ident.position(identOffset + 9); // OpusHead + version
            int channels = ident.readU8();
            ident.readU16LE(); // pre-skip
            int inputSampleRate = (int) ident.readU32LE();
            int sampleRate = inputSampleRate > 0 ? inputSampleRate : OPUS_GRANULE_RATE;
            return new AudioIdent("opus", sampleRate, channels, 0, OPUS_GRANULE_RATE);
        }

        throw new CodecMediaException("Unsupported OGG codec: currently Vorbis and Opus are parsed");
    }

    private static boolean isVorbisIdentification(byte[] data, int offset, int payloadSize) {
        return payloadSize >= 7
                && data[offset] == 0x01
                && data[offset + 1] == 'v'
                && data[offset + 2] == 'o'
                && data[offset + 3] == 'r'
                && data[offset + 4] == 'b'
                && data[offset + 5] == 'i'
                && data[offset + 6] == 's';
    }

    private static boolean isOpusIdentification(byte[] data, int offset, int payloadSize) {
        return payloadSize >= 8
                && data[offset] == 'O'
                && data[offset + 1] == 'p'
                && data[offset + 2] == 'u'
                && data[offset + 3] == 's'
                && data[offset + 4] == 'H'
                && data[offset + 5] == 'e'
                && data[offset + 6] == 'a'
                && data[offset + 7] == 'd';
    }

    private static OggPageHeader parsePageHeader(byte[] data, int offset) {
        if (offset < 0 || offset + 27 > data.length) {
            return null;
        }
        if (data[offset] != 'O' || data[offset + 1] != 'g' || data[offset + 2] != 'g' || data[offset + 3] != 'S') {
            return null;
        }

        ByteArrayReader r = new ByteArrayReader(data);
        r.position(offset + 4);
        int version = r.readU8();
        int headerType = r.readU8();
        long granulePosition = r.readU64LE();
        long serial = r.readU32LE();
        long sequence = r.readU32LE();
        r.readU32LE(); // checksum
        int segmentCount = r.readU8();

        if (offset + 27 + segmentCount > data.length) {
            return null;
        }

        int payload = 0;
        for (int i = 0; i < segmentCount; i++) {
            payload += data[offset + 27 + i] & 0xFF;
        }

        int headerSize = 27 + segmentCount;
        int total = headerSize + payload;
        if (offset + total > data.length) {
            return null;
        }

        return new OggPageHeader(version, headerType, granulePosition, serial, sequence, segmentCount, payload, total, headerSize);
    }

    private record AudioIdent(
            String codec,
            int sampleRate,
            int channels,
            long nominalBitrate,
            int granuleRate
    ) {
    }
}

