package me.tamkungz.codecmedia.internal;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import me.tamkungz.codecmedia.CodecMediaEngine;
import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.internal.audio.mp3.Mp3Parser;
import me.tamkungz.codecmedia.internal.audio.mp3.Mp3ProbeInfo;
import me.tamkungz.codecmedia.internal.audio.ogg.OggParser;
import me.tamkungz.codecmedia.internal.audio.ogg.OggProbeInfo;
import me.tamkungz.codecmedia.internal.audio.wav.WavParser;
import me.tamkungz.codecmedia.internal.audio.wav.WavProbeInfo;
import me.tamkungz.codecmedia.internal.convert.ConversionHub;
import me.tamkungz.codecmedia.internal.convert.ConversionRequest;
import me.tamkungz.codecmedia.internal.convert.DefaultConversionHub;
import me.tamkungz.codecmedia.internal.image.jpeg.JpegParser;
import me.tamkungz.codecmedia.internal.image.jpeg.JpegProbeInfo;
import me.tamkungz.codecmedia.internal.image.png.PngParser;
import me.tamkungz.codecmedia.internal.image.png.PngProbeInfo;
import me.tamkungz.codecmedia.internal.video.mp4.Mp4Parser;
import me.tamkungz.codecmedia.internal.video.mp4.Mp4ProbeInfo;
import me.tamkungz.codecmedia.model.ConversionResult;
import me.tamkungz.codecmedia.model.ExtractionResult;
import me.tamkungz.codecmedia.model.MediaType;
import me.tamkungz.codecmedia.model.Metadata;
import me.tamkungz.codecmedia.model.PlaybackResult;
import me.tamkungz.codecmedia.model.ProbeResult;
import me.tamkungz.codecmedia.model.StreamInfo;
import me.tamkungz.codecmedia.model.StreamKind;
import me.tamkungz.codecmedia.model.ValidationResult;
import me.tamkungz.codecmedia.options.AudioExtractOptions;
import me.tamkungz.codecmedia.options.ConversionOptions;
import me.tamkungz.codecmedia.options.PlaybackOptions;
import me.tamkungz.codecmedia.options.ValidationOptions;

/**
 * Temporary stub implementation to bootstrap API integration.
 */
public final class StubCodecMediaEngine implements CodecMediaEngine {

    private static final long STRICT_VALIDATION_MAX_BYTES = 32L * 1024L * 1024L;
    private final ConversionHub conversionHub = new DefaultConversionHub();

    @Override
    public ProbeResult get(Path input) throws CodecMediaException {
        return probe(input);
    }

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

            if ("wav".equals(extension) || WavParser.isLikelyWav(bytes)) {
                try {
                    WavProbeInfo info = WavParser.parse(bytes);
                    return new ProbeResult(
                            input,
                            "audio/wav",
                            "wav",
                            MediaType.AUDIO,
                            info.durationMillis(),
                            List.of(new StreamInfo(0, StreamKind.AUDIO, "pcm", info.bitrateKbps(), info.sampleRate(), info.channels(), null, null, null)),
                            Map.of("sizeBytes", String.valueOf(size), "bitrateMode", info.bitrateMode().name())
                    );
                } catch (CodecMediaException ignored) {
                    // Fall back to extension-only probe for malformed/partial files.
                }
                return new ProbeResult(input, "audio/wav", "wav", MediaType.AUDIO, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
            }

            if ("png".equals(extension) || PngParser.isLikelyPng(bytes)) {
                try {
                    PngProbeInfo info = PngParser.parse(bytes);
                    return new ProbeResult(
                            input,
                            "image/png",
                            "png",
                            MediaType.IMAGE,
                            null,
                            List.of(new StreamInfo(0, StreamKind.VIDEO, "png", null, null, null, info.width(), info.height(), null)),
                            Map.of(
                                    "sizeBytes", String.valueOf(size),
                                    "bitDepth", String.valueOf(info.bitDepth()),
                                    "colorType", String.valueOf(info.colorType())
                            )
                    );
                } catch (CodecMediaException ignored) {
                    // Fall back to extension-only probe for malformed/partial files.
                }
                return new ProbeResult(input, "image/png", "png", MediaType.IMAGE, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
            }

            if ("jpg".equals(extension) || "jpeg".equals(extension) || JpegParser.isLikelyJpeg(bytes)) {
                String outputExt = "jpeg".equals(extension) ? "jpeg" : "jpg";
                try {
                    JpegProbeInfo info = JpegParser.parse(bytes);
                    return new ProbeResult(
                            input,
                            "image/jpeg",
                            outputExt,
                            MediaType.IMAGE,
                            null,
                            List.of(new StreamInfo(0, StreamKind.VIDEO, "jpeg", null, null, null, info.width(), info.height(), null)),
                            Map.of(
                                    "sizeBytes", String.valueOf(size),
                                    "bitsPerSample", String.valueOf(info.bitsPerSample()),
                                    "channels", String.valueOf(info.channels())
                            )
                    );
                } catch (CodecMediaException ignored) {
                    // Fall back to extension-only probe for malformed/partial files.
                }
                return new ProbeResult(input, "image/jpeg", outputExt, MediaType.IMAGE, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
            }

            if ("mp4".equals(extension) || "m4a".equals(extension) || Mp4Parser.isLikelyMp4(bytes)) {
                String outputExt = "m4a".equals(extension) ? "m4a" : "mp4";
                String mimeType = "m4a".equals(outputExt) ? "audio/mp4" : "video/mp4";
                MediaType mediaType = "m4a".equals(outputExt) ? MediaType.AUDIO : MediaType.VIDEO;
                try {
                    Mp4ProbeInfo info = Mp4Parser.parse(bytes);
                    java.util.LinkedHashMap<String, String> tags = new java.util.LinkedHashMap<>();
                    tags.put("sizeBytes", String.valueOf(size));
                    if (info.majorBrand() != null && !info.majorBrand().isBlank()) {
                        tags.put("majorBrand", info.majorBrand());
                    }

                    List<StreamInfo> streams;
                    if (mediaType == MediaType.VIDEO && info.width() != null && info.height() != null && info.width() > 0 && info.height() > 0) {
                        streams = List.of(new StreamInfo(0, StreamKind.VIDEO, "h264/unknown", null, null, null, info.width(), info.height(), null));
                    } else {
                        streams = List.of();
                    }

                    return new ProbeResult(
                            input,
                            mimeType,
                            outputExt,
                            mediaType,
                            info.durationMillis(),
                            streams,
                            tags
                    );
                } catch (CodecMediaException ignored) {
                    // Fall back to extension-only probe for malformed/partial files.
                }
                return new ProbeResult(input, mimeType, outputExt, mediaType, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
            }

            String mimeType = switch (extension) {
                case "mp4" -> "video/mp4";
                case "m4a" -> "audio/mp4";
                case "mp3" -> "audio/mpeg";
                case "ogg" -> "audio/ogg";
                case "wav" -> "audio/wav";
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                default -> "application/octet-stream";
            };
            MediaType mediaType = switch (extension) {
                case "mp4" -> MediaType.VIDEO;
                case "m4a", "mp3", "ogg", "wav" -> MediaType.AUDIO;
                case "png", "jpg", "jpeg" -> MediaType.IMAGE;
                default -> MediaType.UNKNOWN;
            };

            return new ProbeResult(input, mimeType, extension, mediaType, null, List.of(), Map.of("sizeBytes", String.valueOf(size)));
        } catch (IOException e) {
            throw new CodecMediaException("Failed to probe file: " + input, e);
        }
    }

    @Override
    public Metadata readMetadata(Path input) throws CodecMediaException {
        ensureExists(input);
        ProbeResult probe = probe(input);
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("mimeType", probe.mimeType());
        entries.put("extension", probe.extension());
        entries.put("mediaType", probe.mediaType().name());

        Path sidecar = metadataSidecarPath(input);
        if (Files.exists(sidecar)) {
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(sidecar)) {
                properties.load(in);
            } catch (IOException e) {
                throw new CodecMediaException("Failed to read metadata sidecar: " + sidecar, e);
            }
            for (String key : properties.stringPropertyNames()) {
                entries.putIfAbsent(key, properties.getProperty(key));
            }
        }

        return new Metadata(entries);
    }

    @Override
    public void writeMetadata(Path input, Metadata metadata) throws CodecMediaException {
        ensureExists(input);
        if (metadata == null || metadata.entries() == null) {
            throw new CodecMediaException("Metadata is required");
        }

        for (Map.Entry<String, String> entry : metadata.entries().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new CodecMediaException("Metadata key must not be null/blank");
            }
            if (entry.getValue() == null) {
                throw new CodecMediaException("Metadata value must not be null for key: " + entry.getKey());
            }
        }

        Path sidecar = metadataSidecarPath(input);
        Properties properties = new Properties();
        Map<String, String> sorted = new TreeMap<>(metadata.entries());
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        try (OutputStream out = Files.newOutputStream(sidecar)) {
            properties.store(out, "CodecMedia metadata sidecar");
        } catch (IOException e) {
            throw new CodecMediaException("Failed to write metadata sidecar: " + sidecar, e);
        }
    }

    @Override
    public ExtractionResult extractAudio(Path input, Path outputDir, AudioExtractOptions options) throws CodecMediaException {
        ensureExists(input);
        if (outputDir == null) {
            throw new CodecMediaException("Output directory is required");
        }

        ProbeResult probe = probe(input);
        AudioExtractOptions effective = options != null
                ? options
                : AudioExtractOptions.defaults(normalizeExtension(probe.extension()));
        if (effective.targetFormat() == null || effective.targetFormat().isBlank()) {
            throw new CodecMediaException("AudioExtractOptions.targetFormat is required");
        }

        if (probe.mediaType() != MediaType.AUDIO) {
            throw new CodecMediaException("Input is not an audio file: " + input);
        }

        String sourceExtension = normalizeExtension(probe.extension());
        String requestedExtension = normalizeExtension(effective.targetFormat());
        if (!requestedExtension.equals(sourceExtension)) {
            throw new CodecMediaException(
                    "Stub extractAudio does not transcode. Requested format '" + requestedExtension
                            + "' must match source format '" + sourceExtension + "'"
            );
        }

        try {
            Files.createDirectories(outputDir);
            String baseName = baseName(input.getFileName().toString());
            String extension = sourceExtension;
            Path outputFile = outputDir.resolve(baseName + "_audio." + extension);
            Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING);
            return new ExtractionResult(outputFile, extension);
        } catch (IOException e) {
            throw new CodecMediaException("Failed to extract audio: " + input, e);
        }
    }

    @Override
    public ConversionResult convert(Path input, Path output, ConversionOptions options) throws CodecMediaException {
        ensureExists(input);
        if (output == null) {
            throw new CodecMediaException("Output file is required");
        }

        String sourceExtension = normalizeExtension(extractExtension(input));
        String inferredTargetFormat = extractExtension(output);
        ConversionOptions effective = options != null ? options : ConversionOptions.defaults(inferredTargetFormat);
        if (effective.targetFormat() == null || effective.targetFormat().isBlank()) {
            throw new CodecMediaException("ConversionOptions.targetFormat is required");
        }
        String requestedExtension = normalizeExtension(effective.targetFormat());

        ProbeResult sourceProbe = probe(input);
        me.tamkungz.codecmedia.model.MediaType targetMediaType = mediaTypeByExtension(requestedExtension);
        ConversionRequest request = new ConversionRequest(
                input,
                output,
                sourceExtension,
                requestedExtension,
                sourceProbe.mediaType(),
                targetMediaType,
                effective
        );
        return conversionHub.convert(request);
    }

    @Override
    public PlaybackResult play(Path input, PlaybackOptions options) throws CodecMediaException {
        ensureExists(input);
        ProbeResult probe = probe(input);
        PlaybackOptions effective = options != null ? options : PlaybackOptions.defaults();

        if (probe.mediaType() == MediaType.UNKNOWN) {
            throw new CodecMediaException("Playback is not supported for unknown media type: " + input);
        }

        if (effective.dryRun()) {
            return new PlaybackResult(true, "dry-run", probe.mediaType(), "Playback simulation successful");
        }

        if (effective.allowExternalApp() && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(input.toFile());
                return new PlaybackResult(true, "desktop-open", probe.mediaType(), "Opened with system default application");
            } catch (IOException | RuntimeException e) {
                throw new CodecMediaException("Failed to open media with system player/viewer: " + input, e);
            }
        }

        throw new CodecMediaException("No playback backend available. Try dryRun=true or allowExternalApp=true");
    }

    @Override
    public ValidationResult validate(Path input, ValidationOptions options) {
        ValidationOptions effective = options != null ? options : ValidationOptions.defaults();
        boolean exists = Files.exists(input);
        if (!exists) {
            return new ValidationResult(false, List.of(), List.of("File does not exist: " + input));
        }

        try {
            long size = Files.size(input);
            if (effective.maxBytes() > 0 && size > effective.maxBytes()) {
                return new ValidationResult(
                        false,
                        List.of(),
                        List.of("File exceeds maxBytes: " + size + " > " + effective.maxBytes())
                );
            }

            if (effective.strict()) {
                String extension = extractExtension(input);
                if (size > STRICT_VALIDATION_MAX_BYTES) {
                    return new ValidationResult(
                            false,
                            List.of(),
                            List.of("Strict validation is limited to files <= " + STRICT_VALIDATION_MAX_BYTES + " bytes")
                    );
                }

                byte[] bytes = Files.readAllBytes(input);
                if ("mp3".equals(extension)) {
                    try {
                        Mp3Parser.parse(bytes);
                    } catch (CodecMediaException e) {
                        return new ValidationResult(false, List.of(), List.of("Strict validation failed for mp3: " + e.getMessage()));
                    }
                } else if ("ogg".equals(extension)) {
                    try {
                        OggParser.parse(bytes);
                    } catch (CodecMediaException e) {
                        return new ValidationResult(false, List.of(), List.of("Strict validation failed for ogg: " + e.getMessage()));
                    }
                } else if ("wav".equals(extension)) {
                    try {
                        WavParser.parse(bytes);
                    } catch (CodecMediaException e) {
                        return new ValidationResult(false, List.of(), List.of("Strict validation failed for wav: " + e.getMessage()));
                    }
                } else if ("png".equals(extension)) {
                    try {
                        PngParser.parse(bytes);
                    } catch (CodecMediaException e) {
                        return new ValidationResult(false, List.of(), List.of("Strict validation failed for png: " + e.getMessage()));
                    }
                } else if ("jpg".equals(extension) || "jpeg".equals(extension)) {
                    try {
                        JpegParser.parse(bytes);
                    } catch (CodecMediaException e) {
                        return new ValidationResult(false, List.of(), List.of("Strict validation failed for jpg/jpeg: " + e.getMessage()));
                    }
                } else if ("mp4".equals(extension) || "m4a".equals(extension)) {
                    try {
                        Mp4Parser.parse(bytes);
                    } catch (CodecMediaException e) {
                        return new ValidationResult(false, List.of(), List.of("Strict validation failed for " + extension + ": " + e.getMessage()));
                    }
                }
            }

            return new ValidationResult(true, List.of(), List.of());
        } catch (IOException e) {
            return new ValidationResult(false, List.of(), List.of("Failed to validate file: " + e.getMessage()));
        }
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

    private static Path metadataSidecarPath(Path input) {
        return input.resolveSibling(input.getFileName() + ".codecmedia.properties");
    }

    private static String normalizeExtension(String format) {
        String value = format.trim().toLowerCase(Locale.ROOT);
        return value.startsWith(".") ? value.substring(1) : value;
    }

    private static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static MediaType mediaTypeByExtension(String extension) {
        return switch (normalizeExtension(extension)) {
            case "mp3", "ogg", "wav", "pcm", "m4a", "aac", "flac" -> MediaType.AUDIO;
            case "mp4", "mkv", "mov", "avi", "webm" -> MediaType.VIDEO;
            case "png", "jpg", "jpeg", "gif", "bmp", "webp" -> MediaType.IMAGE;
            default -> MediaType.UNKNOWN;
        };
    }
}
