package me.tamkungz.codecmedia;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.imageio.ImageIO;

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
    void convert_shouldRejectVideoToAudioRouteForNow() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp4 = createTempFileWithResource("mp4_test.mp4", ".mp4");
        Path outputMp3 = Files.createTempFile("codecmedia-video-to-audio-", ".mp3");

        try {
            boolean threw = false;
            try {
                engine.convert(tempMp4, outputMp3, new me.tamkungz.codecmedia.options.ConversionOptions("mp3", "balanced", true));
            } catch (CodecMediaException expected) {
                threw = expected.getMessage().contains("video->audio");
            }
            assertTrue(threw);
        } finally {
            Files.deleteIfExists(outputMp3);
            Files.deleteIfExists(tempMp4);
        }
    }

    @Test
    void convert_shouldRejectAudioToImageRouteForNow() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");
        Path outputPng = Files.createTempFile("codecmedia-audio-to-image-", ".png");

        try {
            boolean threw = false;
            try {
                engine.convert(tempMp3, outputPng, new me.tamkungz.codecmedia.options.ConversionOptions("png", "balanced", true));
            } catch (CodecMediaException expected) {
                threw = expected.getMessage().contains("audio->image");
            }
            assertTrue(threw);
        } finally {
            Files.deleteIfExists(outputPng);
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void convert_shouldTranscodePngToJpg() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempPng = createTempFileWithResource("png_test.png", ".png");
        Path outputJpg = Files.createTempFile("codecmedia-png-to-jpg-", ".jpg");

        try {
            var converted = engine.convert(tempPng, outputJpg, new me.tamkungz.codecmedia.options.ConversionOptions("jpg", "balanced", true));
            assertEquals("jpg", converted.format());
            assertTrue(converted.reencoded());
            assertTrue(Files.exists(outputJpg));
            assertTrue(Files.size(outputJpg) > 0);

            var probed = engine.probe(outputJpg);
            assertEquals("image/jpeg", probed.mimeType());
            assertEquals("jpg", probed.extension());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.IMAGE, probed.mediaType());
        } finally {
            Files.deleteIfExists(outputJpg);
            Files.deleteIfExists(tempPng);
        }
    }

    @Test
    void convert_shouldTranscodeJpgToPng() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempJpg = createTempJpegFixture(".jpg");
        Path outputPng = Files.createTempFile("codecmedia-jpg-to-png-", ".png");

        try {
            var converted = engine.convert(tempJpg, outputPng, new me.tamkungz.codecmedia.options.ConversionOptions("png", "balanced", true));
            assertEquals("png", converted.format());
            assertTrue(converted.reencoded());
            assertTrue(Files.exists(outputPng));
            assertTrue(Files.size(outputPng) > 0);

            var probed = engine.probe(outputPng);
            assertEquals("image/png", probed.mimeType());
            assertEquals("png", probed.extension());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.IMAGE, probed.mediaType());
        } finally {
            Files.deleteIfExists(outputPng);
            Files.deleteIfExists(tempJpg);
        }
    }

    @Test
    void convert_shouldRejectVideoToVideoTranscodeForNow() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp4 = createTempFileWithResource("mp4_test.mp4", ".mp4");
        Path outputMkv = Files.createTempFile("codecmedia-video-to-video-", ".mkv");

        try {
            boolean threw = false;
            try {
                engine.convert(tempMp4, outputMkv, new me.tamkungz.codecmedia.options.ConversionOptions("mkv", "balanced", true));
            } catch (CodecMediaException expected) {
                threw = expected.getMessage().contains("video->video");
            }
            assertTrue(threw);
        } finally {
            Files.deleteIfExists(outputMkv);
            Files.deleteIfExists(tempMp4);
        }
    }

    @Test
    void convert_shouldRejectAudioToAudioTranscodeForNow() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");
        Path outputWav = Files.createTempFile("codecmedia-audio-to-audio-", ".wav");

        try {
            boolean threw = false;
            try {
                engine.convert(tempMp3, outputWav, new me.tamkungz.codecmedia.options.ConversionOptions("wav", "balanced", true));
            } catch (CodecMediaException expected) {
                threw = expected.getMessage().contains("audio->audio");
            }
            assertTrue(threw);
        } finally {
            Files.deleteIfExists(outputWav);
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void convert_shouldAllowWavToPcmStubRoute() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempWav = createTempFileWithResource("c-major-scale_test_ableton-live.wav", ".wav");
        Path outputPcm = Files.createTempFile("codecmedia-wav-to-pcm-", ".pcm");

        try {
            var converted = engine.convert(tempWav, outputPcm, new me.tamkungz.codecmedia.options.ConversionOptions("pcm", "balanced", true));
            assertEquals("pcm", converted.format());
            assertFalse(converted.reencoded());
            assertTrue(Files.exists(outputPcm));
            assertTrue(Files.size(outputPcm) > 0);
        } finally {
            Files.deleteIfExists(outputPcm);
            Files.deleteIfExists(tempWav);
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

    @Test
    void validate_strictShouldAcceptValidWavFixture() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempWav = createTempFileWithResource("c-major-scale_test_ableton-live.wav", ".wav");

        try {
            var result = engine.validate(tempWav, new me.tamkungz.codecmedia.options.ValidationOptions(true, 0));
            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
        } finally {
            Files.deleteIfExists(tempWav);
        }
    }

    @Test
    void probe_shouldDetectPngBySignature() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempPng = createTempFileWithResource("png_test.png", ".png");

        try {
            var result = engine.probe(tempPng);
            assertEquals("image/png", result.mimeType());
            assertEquals("png", result.extension());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.IMAGE, result.mediaType());
            assertFalse(result.streams().isEmpty());
            assertNotNull(result.streams().get(0).width());
            assertNotNull(result.streams().get(0).height());
            assertTrue(result.streams().get(0).width() > 0);
            assertTrue(result.streams().get(0).height() > 0);
        } finally {
            Files.deleteIfExists(tempPng);
        }
    }

    @Test
    void validate_strictShouldAcceptValidPngFixture() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempPng = createTempFileWithResource("png_test.png", ".png");

        try {
            var result = engine.validate(tempPng, new me.tamkungz.codecmedia.options.ValidationOptions(true, 0));
            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
        } finally {
            Files.deleteIfExists(tempPng);
        }
    }

    @Test
    void probe_shouldDetectJpegBySignature() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempJpeg = createTempJpegFixture(".jpg");

        try {
            var result = engine.probe(tempJpeg);
            assertEquals("image/jpeg", result.mimeType());
            assertEquals("jpg", result.extension());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.IMAGE, result.mediaType());
            assertFalse(result.streams().isEmpty());
            assertEquals(1, result.streams().get(0).width());
            assertEquals(1, result.streams().get(0).height());
        } finally {
            Files.deleteIfExists(tempJpeg);
        }
    }

    @Test
    void probe_shouldRespectJpegExtensionVariant() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempJpeg = createTempJpegFixture(".jpeg");

        try {
            var result = engine.probe(tempJpeg);
            assertEquals("image/jpeg", result.mimeType());
            assertEquals("jpeg", result.extension());
        } finally {
            Files.deleteIfExists(tempJpeg);
        }
    }

    @Test
    void validate_strictShouldAcceptValidJpegFixture() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempJpeg = createTempJpegFixture(".jpg");

        try {
            var result = engine.validate(tempJpeg, new me.tamkungz.codecmedia.options.ValidationOptions(true, 0));
            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
        } finally {
            Files.deleteIfExists(tempJpeg);
        }
    }

    @Test
    void probe_shouldDetectMp4AndProvideBasicVideoInfo() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp4 = createTempFileWithResource("mp4_test.mp4", ".mp4");

        try {
            var result = engine.probe(tempMp4);
            assertEquals("video/mp4", result.mimeType());
            assertEquals("mp4", result.extension());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.VIDEO, result.mediaType());
            assertNotNull(result.tags());
            assertTrue(result.tags().containsKey("sizeBytes"));
            assertTrue(result.tags().containsKey("majorBrand"));
            if (result.durationMillis() != null) {
                assertTrue(result.durationMillis() > 0);
            }
            if (!result.streams().isEmpty()) {
                assertNotNull(result.streams().get(0).width());
                assertNotNull(result.streams().get(0).height());
                assertTrue(result.streams().get(0).width() > 0);
                assertTrue(result.streams().get(0).height() > 0);
            }
        } finally {
            Files.deleteIfExists(tempMp4);
        }
    }

    @Test
    void validate_strictShouldAcceptValidMp4Fixture() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp4 = createTempFileWithResource("mp4_test.mp4", ".mp4");

        try {
            var result = engine.validate(tempMp4, new me.tamkungz.codecmedia.options.ValidationOptions(true, 0));
            assertTrue(result.valid());
            assertTrue(result.errors().isEmpty());
        } finally {
            Files.deleteIfExists(tempMp4);
        }
    }

    @Test
    void get_shouldAliasProbe() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");

        try {
            var a = engine.probe(tempMp3);
            var b = engine.get(tempMp3);
            assertEquals(a.mimeType(), b.mimeType());
            assertEquals(a.extension(), b.extension());
            assertEquals(a.mediaType(), b.mediaType());
        } finally {
            Files.deleteIfExists(tempMp3);
        }
    }

    @Test
    void play_shouldSupportDryRunForAudioImageVideo() throws Exception {
        CodecMediaEngine engine = CodecMedia.createDefault();
        Path tempMp3 = createTempFileWithResource("c-major-scale_test_audacity.mp3", ".mp3");
        Path tempPng = createTempFileWithResource("png_test.png", ".png");
        Path tempMp4 = createTempFileWithResource("mp4_test.mp4", ".mp4");

        try {
            var audioPlay = engine.play(tempMp3, new me.tamkungz.codecmedia.options.PlaybackOptions(true, false));
            assertTrue(audioPlay.started());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.AUDIO, audioPlay.mediaType());

            var imagePlay = engine.play(tempPng, new me.tamkungz.codecmedia.options.PlaybackOptions(true, false));
            assertTrue(imagePlay.started());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.IMAGE, imagePlay.mediaType());

            var videoPlay = engine.play(tempMp4, new me.tamkungz.codecmedia.options.PlaybackOptions(true, false));
            assertTrue(videoPlay.started());
            assertEquals(me.tamkungz.codecmedia.model.MediaType.VIDEO, videoPlay.mediaType());
        } finally {
            Files.deleteIfExists(tempMp3);
            Files.deleteIfExists(tempPng);
            Files.deleteIfExists(tempMp4);
        }
    }

    private static Path createTempFileWithResource(String resourceName, String suffix) throws IOException {
        Path resource = Path.of("src/test/resources", resourceName);
        Path temp = Files.createTempFile("codecmedia-fixture-", suffix);
        Files.copy(resource, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return temp;
    }

    private static Path createTempJpegFixture(String suffix) throws IOException {
        Path temp = Files.createTempFile("codecmedia-jpeg-", suffix);
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFFFFFF);
        boolean ok = ImageIO.write(image, "jpg", temp.toFile());
        if (!ok) {
            throw new IOException("No JPEG writer available in ImageIO runtime");
        }
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
