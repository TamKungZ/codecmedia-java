package me.tamkungz.codecmedia.internal.audio.ogg;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.BitrateMode;
import me.tamkungz.codecmedia.internal.io.ByteArrayReader;

public final class OggParser {

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
        if (identOffset + 30 > data.length) {
            throw new CodecMediaException("Invalid OGG stream: incomplete Vorbis identification packet");
        }

        if (data[identOffset] != 0x01
                || data[identOffset + 1] != 'v'
                || data[identOffset + 2] != 'o'
                || data[identOffset + 3] != 'r'
                || data[identOffset + 4] != 'b'
                || data[identOffset + 5] != 'i'
                || data[identOffset + 6] != 's') {
            throw new CodecMediaException("Unsupported OGG codec: currently only Vorbis is parsed");
        }

        ByteArrayReader ident = new ByteArrayReader(data);
        ident.position(identOffset + 7);
        ident.readU32LE(); // vorbis version
        int channels = ident.readU8();
        int sampleRate = (int) ident.readU32LE();
        long bitrateNominal = ident.readU32LE();

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

        long durationMillis = (sampleRate > 0 && maxGranule > 0)
                ? (maxGranule * 1000L) / sampleRate
                : 0;

        int avgBitrate = durationMillis > 0
                ? (int) ((payloadBits * 1000L) / durationMillis / 1000L)
                : 0;

        int nominalKbps = bitrateNominal > 0 ? (int) (bitrateNominal / 1000L) : 0;
        int bitrateKbps = avgBitrate > 0 ? avgBitrate : nominalKbps;

        BitrateMode mode = (bitrateNominal > 0 || pageCount > 2)
                ? BitrateMode.VBR
                : BitrateMode.UNKNOWN;

        return new OggProbeInfo("vorbis", sampleRate, channels, bitrateKbps, mode, durationMillis);
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
}

