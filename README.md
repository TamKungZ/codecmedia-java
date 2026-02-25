# CodecMedia

CodecMedia is a Java library for media probing, validation, metadata sidecar persistence, audio extraction, playback workflow simulation, and conversion routing.

<p align="center">
  <img src="https://www.tamkungz.me/assets-image/CodecMedia_Full_Logo.png" width="70%" alt="CodecMedia Logo">
</p>

## Features

- Media engine facade via `CodecMedia.createDefault()`
- Probing support for:
  - MP3
  - OGG/Vorbis
  - WAV (RIFF/WAVE)
  - PNG
  - JPEG
  - WebP
  - BMP
  - TIFF
  - HEIC/HEIF/AVIF (basic BMFF parsing)
  - MP4 (basic ISO BMFF parsing)
- Validation with size limits and strict parser checks for MP3/OGG/WAV/PNG/JPEG/WebP/BMP/TIFF/HEIC/HEIF/AVIF/MP4
- Metadata read/write with sidecar persistence (`.codecmedia.properties`)
- In-Java extraction and conversion file operations
- Image-to-image conversion in Java for: `png`, `jpg`/`jpeg`, `webp`, `bmp`, `tif`/`tiff`, `heic`/`heif`
- Playback API with dry-run support and optional desktop-open backend
- Conversion hub routing with explicit unsupported routes and a stub `wav <-> pcm` path

## API Behavior Summary

- `get(input)`: alias of `probe(input)` for convenience.
- `probe(input)`: detects media/container characteristics and returns technical stream info for supported formats.
- `readMetadata(input)`: returns derived probe metadata plus sidecar entries when present.
- `writeMetadata(input, metadata)`: validates and writes metadata to a sidecar properties file next to the input.
- `extractAudio(input, outputDir, options)`: validates audio input and writes extracted output into `outputDir`.
- `convert(input, output, options)`: performs routed conversion behavior and enforces `overwrite` handling.
- `play(input, options)`: supports dry-run playback and optional system default app launch.
- `validate(input, options)`: validates existence, max size, and optional strict parser-level checks.

## Notes and Limitations

- Current probing focuses on **technical media info** (mime/type/streams/basic tags).
- `readMetadata` currently uses sidecar metadata persistence; it is **not** a full embedded tag extractor (for example ID3 album art/APIC).
- Audio-to-audio conversion is not implemented yet for real transcode cases (for example `mp3 -> ogg`).
- The only temporary audio conversion path is a stub `wav <-> pcm` route.
- For OpenAL workflows that require OGG from MP3 input, use an external transcoder first (for example ffmpeg), then play the produced OGG.

## Requirements

- Java 17+
- Maven 3.9+

## Build

```bash
mvn clean test
```

## Fixture-Driven Tests

Fixtures are in `src/test/resources`, including:

- `c-major-scale_test_ableton-live.wav`
- `c-major-scale_test_audacity.mp3`
- `c-major-scale_test_ffmpeg.ogg`
- `c-major-scale_test_web-convert_mono.mp3`
- `mp4_test.mp4`
- `png_test.png`

Run probe fixture test only:

```bash
mvn -Dtest=CodecMediaPlayTest test
```

Run facade-focused tests only:

```bash
mvn -Dtest=CodecMediaFacadeTest test
```

## Full Test Suite

```bash
mvn test
```

## License

This project is licensed under the Apache License 2.0.

---

*by TamKungZ_*