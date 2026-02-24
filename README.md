# CodecMedia

CodecMedia is a Java library for media probing, validation, metadata sidecar persistence, audio extraction, and file conversion workflows.

## Features

- Media probing facade via `CodecMedia.createDefault()`
- MP3 frame parsing support
- OGG/Vorbis page parsing support
- Validation with size limits and strict parser checks for MP3/OGG
- Metadata read/write with sidecar persistence (`.codecmedia.properties`)
- In-Java audio extraction and conversion file operations (no external tools required)

## API Behavior Summary

- `probe(input)`: detects media/container characteristics and stream info for supported formats.
- `readMetadata(input)`: returns derived probe metadata plus sidecar metadata entries when present.
- `writeMetadata(input, metadata)`: validates and writes metadata to a sidecar properties file next to the input.
- `extractAudio(input, outputDir, options)`: validates audio input and writes extracted output into `outputDir`.
- `convert(input, output, options)`: writes conversion output and enforces `overwrite` behavior.
- `validate(input, options)`: validates existence, max size, and optionally strict parser-level checks.

## Requirements

- Java 17+
- Maven 3.9+

## Build

```bash
mvn clean test
```

## Play/Probe Fixture Test

The repository includes a fixture-driven test that probes real audio files under `src/test/resources`:

- `c-major-scale_test_ableton-live.wav`
- `c-major-scale_test_audacity.mp3`
- `c-major-scale_test_ffmpeg.ogg`
- `c-major-scale_test_web-convert_mono.mp3`

Run only this test:

```bash
mvn -Dtest=CodecMediaPlayTest test
```

## Full Test Suite

```bash
mvn test
```

## License

This project is licensed under the Apache License 2.0.
