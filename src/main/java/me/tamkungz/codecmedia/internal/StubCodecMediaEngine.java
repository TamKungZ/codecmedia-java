package me.tamkungz.codecmedia.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.tamkungz.codecmedia.CodecMediaEngine;
import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.mp3.Mp3Parser;
import me.tamkungz.codecmedia.internal.audio.mp3.Mp3ProbeInfo;
import me.tamkungz.codecmedia.internal.audio.ogg.OggParser;
import me.tamkungz.codecmedia.internal.audio.ogg.OggProbeInfo;
import me.tamkungz.codecmedia.model.ConversionResult;
import me.tamkungz.codecmedia.model.ExtractionResult;
import me.tamkungz.codecmedia.model.MediaType;
import me.tamkungz.codecmedia.model.Metadata;
import me.tamkungz.codecmedia.model.ProbeResult;
import me.tamkungz.codecmedia.model.StreamInfo;
import me.tamkungz.codecmedia.model.StreamKind;
import me.tamkungz.codecmedia.model.ValidationResult;
import me.tamkungz.codecmedia.options.AudioExtractOptions;
import me.tamkungz.codecmedia.options.ConversionOptions;
import me.tamkungz.codecmedia.options.ValidationOptions;

/**
 * Temporary stub implementation to bootstrap API integration.
 */
public final class StubCodecMediaEngine implements CodecMediaEngine {

    @Override
    public ProbeResult probe(Path input) throws CodecMediaException {
        ensureExists(input);
        String extension = extractExtension(input);

        try {
            long size = Files.size(input);
            byte[] bytes = Files.readAllBytes(input);

            if ("mp3".equals(extension) || isLikelyMp3(bytes)) {
                if (bytes.length >= 4) {
                    try {
                        Mp3ProbeInfo info = Mp3Parser.parse(bytes);
                        return new ProbeResult(
                                input,
                                "audio/mpeg",
                                "mp3",
                                MediaType.AUDIO,
                                info.durationMillis(),
                                List.of(new StreamInfo(0, StreamKind.AUDIO, info.codec(), info.bitrateKbps(), info.sampleRate(), info.channels(), null, null, null)),
                                Map.of(
                                        "sizeBytes", String.valueOf(size),
                                        "bitrateMode", info.bitrateMode().name()
                                )
                        );
                    } catch (CodecMediaException ignored) {
                        // Fall back to extension-only probe for partial/empty temporary files.
                    }
                }
                return new ProbeResult(input, "audio/mpeg", "mp3", MediaType.AUDIO, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
            }

            if ("ogg".equals(extension) || isLikelyOgg(bytes)) {
                OggProbeInfo info = OggParser.parse(bytes);
                return new ProbeResult(
                        input,
                        "audio/ogg",
                        "ogg",
                        MediaType.AUDIO,
                        info.durationMillis(),
                        List.of(new StreamInfo(0, StreamKind.AUDIO, info.codec(), info.bitrateKbps(), info.sampleRate(), info.channels(), null, null, null)),
                        Map.of(
                                "sizeBytes", String.valueOf(size),
                                "bitrateMode", info.bitrateMode().name()
                        )
                );
            }

            String mimeType = switch (extension) {
                case "mp4" -> "video/mp4";
                case "mp3" -> "audio/mpeg";
                case "ogg" -> "audio/ogg";
                case "png" -> "image/png";
                default -> "application/octet-stream";
            };
            MediaType mediaType = switch (extension) {
                case "mp4" -> MediaType.VIDEO;
                case "mp3", "ogg" -> MediaType.AUDIO;
                case "png" -> MediaType.IMAGE;
                default -> MediaType.UNKNOWN;
            };

            return new ProbeResult(input, mimeType, extension, mediaType, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
        } catch (IOException e) {
            throw new CodecMediaException("Failed to probe file: " + input, e);
        }
    }

    @Override
    public Metadata readMetadata(Path input) throws CodecMediaException {
        ProbeResult probe = probe(input);
        return new Metadata(Map.of(
                "mimeType", probe.mimeType(),
                "extension", probe.extension(),
                "mediaType", probe.mediaType().name()
        ));
    }

    @Override
    public void writeMetadata(Path input, Metadata metadata) throws CodecMediaException {
        ensureExists(input);
        if (metadata == null || metadata.entries() == null) {
            throw new CodecMediaException("Metadata is required");
        }
        throw new CodecMediaException("writeMetadata is not implemented yet");
    }

    @Override
    public ExtractionResult extractAudio(Path input, Path outputDir, AudioExtractOptions options) throws CodecMediaException {
        ensureExists(input);
        throw new CodecMediaException("extractAudio is not implemented yet");
    }

    @Override
    public ConversionResult convert(Path input, Path output, ConversionOptions options) throws CodecMediaException {
        ensureExists(input);
        throw new CodecMediaException("convert is not implemented yet");
    }

    @Override
    public ValidationResult validate(Path input, ValidationOptions options) {
        boolean exists = Files.exists(input);
        if (!exists) {
            return new ValidationResult(false, List.of(), List.of("File does not exist: " + input));
        }
        return new ValidationResult(true, List.of(), List.of());
    }

    private static void ensureExists(Path input) throws CodecMediaException {
        if (!Files.exists(input)) {
            throw new CodecMediaException("File does not exist: " + input);
        }
    }

    private static String extractExtension(Path input) {
        String name = input.getFileName().toString();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == name.length() - 1) {
            return "";
        }
        return name.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isLikelyOgg(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 'O'
                && bytes[1] == 'g'
                && bytes[2] == 'g'
                && bytes[3] == 'S';
    }

    private static boolean isLikelyMp3(byte[] bytes) {
        if (bytes.length < 3) {
            return false;
        }
        if (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') {
            return true;
        }
        if (bytes.length < 2) {
            return false;
        }
        return (bytes[0] & (byte) 0xFF) == (byte) 0xFF && (bytes[1] & (byte) 0xE0) == (byte) 0xE0;
    }
}
