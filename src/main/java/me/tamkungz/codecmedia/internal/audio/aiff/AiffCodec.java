package me.tamkungz.codecmedia.internal.audio.aiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import me.tamkungz.codecmedia.CodecMediaException;

public final class AiffCodec {

    private AiffCodec() {
    }

    public static AiffProbeInfo decode(Path input) throws CodecMediaException {
        try {
            byte[] bytes = Files.readAllBytes(input);
            return decode(bytes, input);
        } catch (IOException e) {
            throw new CodecMediaException("Failed to decode AIFF: " + input, e);
        }
    }

    public static AiffProbeInfo decode(byte[] bytes, Path sourceRef) throws CodecMediaException {
        AiffProbeInfo info = AiffParser.parse(bytes);
        validateDecodedProbe(info, sourceRef);
        return info;
    }

    private static void validateDecodedProbe(AiffProbeInfo info, Path input) throws CodecMediaException {
        if (info.sampleRate() <= 0 || info.channels() <= 0 || info.bitrateKbps() <= 0) {
            throw new CodecMediaException("Decoded AIFF has invalid stream values: " + input);
        }
    }
}

