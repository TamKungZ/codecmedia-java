package me.tamkungz.codecmedia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CodecMediaFacadeTest {

    @Test
    void createDefault_shouldReturnEngine() {
        CodecMediaEngine engine = CodecMedia.createDefault();
        assertNotNull(engine);
    }

    @Test
    void probe_shouldDetectMp3ByExtension() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempFile = Files.createTempFile("codecmedia-", ".mp3");

        try {
            var result = engine.probe(tempFile);
            assertEquals("audio/mpeg", result.mimeType());
            assertEquals("mp3", result.extension());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void validate_shouldFailWhenFileMissing() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path missing = Path.of("this-file-should-not-exist-12345.mp4");

        var result = engine.validate(missing, null);
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("does not exist")));
    }

    @Test
    void writeAndReadMetadata_shouldRoundTripViaSidecar() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");

        try {
            engine.writeMetadata(tempMp3, new me.tamkungz.codecmedia.model.Metadata(Map.of(
                    "title", "C Major Scale",
                    "artist", "CodecMedia Test"
            )));

            var metadata = engine.readMetadata(tempMp3);
            assertEquals("C Major Scale", metadata.entries().get("title"));
            assertEquals("CodecMedia Test", metadata.entries().get("artist"));
            assertEquals("audio/mpeg", metadata.entries().get("mimeType"));
        } finally {
            Files.deleteIfExists(tempMp3.resolveSibling(tempMp3.getFileName() + ".codecmedia.properties"));
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void extractAudio_shouldCreateOutputFile() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_web-convert_mono.mp3", ".mp3");
        Path outputDir = Files.createTempDirectory("codecmedia-extract-");

        try {
            var result = engine.extractAudio(
                    tempMp3,
                    outputDir,
                    new me.tamkungz.codecmedia.options.AudioExtractOptions("mp3", 192, 0)
            );
            assertNotNull(result.outputFile());
            assertTrue(Files.exists(result.outputFile()));
            assertEquals("mp3", result.format());
        } finally {
            deleteDirectory(outputDir);
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void convert_shouldRespectOverwriteFlag() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");
        Path output = Files.createTempFile("codecmedia-convert-", ".mp3");

        try {
            var converted = engine.convert(tempMp3, output, new me.tamkungz.codecmedia.options.ConversionOptions("mp3", "balanced", true));
            assertTrue(Files.size(output) > 0);
            assertFalse(converted.reencoded());

            boolean threw = false;
            try {
                engine.convert(tempMp3, output, new me.tamkungz.codecmedia.options.ConversionOptions("mp3", "balanced", false));
            } catch (CodecMediaException expected) {
                threw = true;
            }
            assertTrue(threw);
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void validate_shouldRespectMaxBytesOption() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");

        try {
            long fileSize = Files.size(tempMp3);
            var result = engine.validate(tempMp3, new me.tamkungz.codecmedia.options.ValidationOptions(false, fileSize - 1));
            assertFalse(result.valid());
            assertTrue(result.errors().stream().anyMatch(e -> e.contains("exceeds maxBytes")));
        } finally {
            Files.deleteIfExists(tempMp3);
        }
    }

    private static Path createTempFileWithResource(String resourceName, String suffix) throws IOException {
        Path resource = Path.of("src/test/resources", resourceName);
        Path temp = Files.createTempFile("codecmedia-fixture-", suffix);
        Files.copy(resource, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return temp;
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // ignore cleanup errors in tests
                }
            });
        }
    }
}
