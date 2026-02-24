package me.tamkungz.codecmedia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
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
}
