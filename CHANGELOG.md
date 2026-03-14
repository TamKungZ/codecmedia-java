# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1] - 2026-03-14

### Fixed
- Improved MP3 duration estimation in [`Mp3Parser.estimateDurationMillis()`](src/main/java/me/tamkungz/codecmedia/internal/audio/mp3/Mp3Parser.java) to prioritize Xing/VBRI frame-count metadata before scanned sample totals.
- Excluded trailing ID3v1 tag bytes from MP3 audio scan range in [`Mp3Parser`](src/main/java/me/tamkungz/codecmedia/internal/audio/mp3/Mp3Parser.java), reducing bitrate drift when footer tags are present.
- Added clearer non-Layer III error handling in [`Mp3Parser.parse()`](src/main/java/me/tamkungz/codecmedia/internal/audio/mp3/Mp3Parser.java) for MPEG Layer I/II inputs.
- Strengthened OGG logical-stream parsing in [`OggParser`](src/main/java/me/tamkungz/codecmedia/internal/audio/ogg/OggParser.java) with per-stream page-sequence validation and serial-scoped metrics for multiplexed files.
- Refined Vorbis bitrate-mode classification in [`OggParser.detectVorbisBitrateMode()`](src/main/java/me/tamkungz/codecmedia/internal/audio/ogg/OggParser.java) to infer from observed bitrate variation instead of coarse nominal/page-count heuristics.
- Replaced broad OGG payload string scanning with structured Vorbis/Opus comment-header parsing in [`OggParser`](src/main/java/me/tamkungz/codecmedia/internal/audio/ogg/OggParser.java), and fixed sequence tracking to use `long` to avoid overflow.

### Added
- Added MP3 parser regression tests for Xing-priority duration, trailing ID3v1 handling, and unsupported Layer I/II diagnostics in [`Mp3ParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/mp3/Mp3ParserTest.java).
- Added OGG parser tests for Vorbis CBR/VBR mode inference, broken page-sequence detection, and multiplexed-stream metric isolation in [`OggParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/ogg/OggParserTest.java).

### Verified
- Confirmed MP3 parser updates with `mvn -Dtest=Mp3ParserTest test`.
- Confirmed OGG parser updates with `mvn -Dtest=OggParserTest test`.

## [1.1.0] - 2026-03-13

### Changed
- Migrated repository ownership and project references to `CodecMediaLib/codecmedia-java`.
- Updated SCM/issue/CI metadata in [`pom.xml`](pom.xml).
- Updated organization metadata in [`pom.xml`](pom.xml) to `CodecMediaLib`.

### Notes
- This `1.1.0` release is a transition baseline and prepares for larger updates planned in the `1.1.*` series.

## [1.0.4] - 2026-03-05

### Added
- Added richer MOV probe fields in [`MovProbeInfo`](src/main/java/me/tamkungz/codecmedia/internal/video/mov/MovProbeInfo.java): `videoBitrateKbps`, `audioBitrateKbps`, `bitDepth`, and `displayAspectRatio`.
- Added richer MP4 probe fields in [`Mp4ProbeInfo`](src/main/java/me/tamkungz/codecmedia/internal/video/mp4/Mp4ProbeInfo.java): codec/audio stream details, frame rate, bitrate fields, bit depth, and display aspect ratio.
- Added richer WebM probe fields in [`WebmProbeInfo`](src/main/java/me/tamkungz/codecmedia/internal/video/webm/WebmProbeInfo.java): per-track bitrate fields and display aspect ratio.
- Added real fixture coverage in [`CodecMediaFacadeTest`](src/test/java/me/tamkungz/codecmedia/CodecMediaFacadeTest.java) for probe/strict-validate using files under [`src/test/resources/example`](src/test/resources/example).
- Added round-trip conversion test class [`CodecMediaRoundTripConversionTest`](src/test/java/me/tamkungz/codecmedia/CodecMediaRoundTripConversionTest.java) to validate same-extension convert->convert flows across all real example extensions (`mp3`, `mp4`, `png`, `webm`).

### Changed
- Enhanced MOV parsing in [`MovParser`](src/main/java/me/tamkungz/codecmedia/internal/video/mov/MovParser.java) with deeper BMFF track metadata extraction (`hdlr`, `mdhd`, `stsd`, `btrt`, `stsz`) and fallback bitrate estimation.
- Enhanced MP4 parsing in [`Mp4Parser`](src/main/java/me/tamkungz/codecmedia/internal/video/mp4/Mp4Parser.java) to extract video/audio codec, sample rate, channels, frame rate, bit depth, bitrate, and aspect ratio.
- Enhanced WebM parsing in [`WebmParser`](src/main/java/me/tamkungz/codecmedia/internal/video/webm/WebmParser.java) to extract track bitrate when present and compute fallback bitrate/aspect ratio values.
- Updated stream/tag mapping in [`StubCodecMediaEngine`](src/main/java/me/tamkungz/codecmedia/internal/StubCodecMediaEngine.java) so MOV/MP4/WebM probe results now expose richer stream bitrate and container tags (`displayAspectRatio`, `bitDepth`, `videoBitrateKbps`, `audioBitrateKbps`).

### Verified
- Confirmed test stability after video parser improvements with `mvn test`.
- Confirmed real-fixture conversion regression path with `mvn -Dtest=CodecMediaRoundTripConversionTest test`.

## [1.0.3] - 2026-03-05

### Added
- Added OGG Opus identification/probing support alongside Vorbis in [`OggParser`](src/main/java/me/tamkungz/codecmedia/internal/audio/ogg/OggParser.java).
- Added AIFF/AIF/AIFC probing support with new parser/codec in [`AiffParser`](src/main/java/me/tamkungz/codecmedia/internal/audio/aiff/AiffParser.java) and [`AiffCodec`](src/main/java/me/tamkungz/codecmedia/internal/audio/aiff/AiffCodec.java).
- Added FLAC probing support with STREAMINFO parsing in [`FlacParser`](src/main/java/me/tamkungz/codecmedia/internal/audio/flac/FlacParser.java) and [`FlacCodec`](src/main/java/me/tamkungz/codecmedia/internal/audio/flac/FlacCodec.java).
- Added parser test coverage for MP3 VBR and mono channel-mode paths in [`Mp3ParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/mp3/Mp3ParserTest.java).
- Added parser test coverage for OGG Opus identification in [`OggParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/ogg/OggParserTest.java).
- Added WAV parser synthetic profile tests (mono/stereo, sample-rate, bit-depth combinations) in [`WavParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/wav/WavParserTest.java).
- Added parser test coverage for AIFF probing in [`AiffParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/aiff/AiffParserTest.java).
- Added facade test coverage for `.m4a` probe/strict-validate flows in [`CodecMediaFacadeTest`](src/test/java/me/tamkungz/codecmedia/CodecMediaFacadeTest.java).
- Added parser/facade test coverage for `.flac` in [`FlacParserTest`](src/test/java/me/tamkungz/codecmedia/internal/audio/flac/FlacParserTest.java) and [`CodecMediaFacadeTest`](src/test/java/me/tamkungz/codecmedia/CodecMediaFacadeTest.java).

### Changed
- Improved probe routing in [`StubCodecMediaEngine`](src/main/java/me/tamkungz/codecmedia/internal/StubCodecMediaEngine.java) to perform lightweight prefix-based type sniffing before full-file decode, reducing unnecessary full reads for unsupported/unknown inputs.
- Extended probe and strict validation routing in [`StubCodecMediaEngine`](src/main/java/me/tamkungz/codecmedia/internal/StubCodecMediaEngine.java) to include AIFF/AIF/AIFC.
- Extended probe and strict validation routing in [`StubCodecMediaEngine`](src/main/java/me/tamkungz/codecmedia/internal/StubCodecMediaEngine.java) to include FLAC.
- Expanded MP4 signature acceptance for M4A family brands in [`Mp4Parser.isLikelyMp4()`](src/main/java/me/tamkungz/codecmedia/internal/video/mp4/Mp4Parser.java).
- Updated feature notes in [`README.md`](README.md) to reflect OGG Vorbis/Opus probing support and prefix-sniff probe behavior.

## [1.0.2] - 2026-03-02

### Added
- Added MOV parser/probe support via [`MovParser`](src/main/java/me/tamkungz/codecmedia/internal/video/mov/MovParser.java).
- Added WebM parser/probe support via [`WebmParser`](src/main/java/me/tamkungz/codecmedia/internal/video/webm/WebmParser.java).
- Added MOV/WebM codec wrappers ([`MovCodec`](src/main/java/me/tamkungz/codecmedia/internal/video/mov/MovCodec.java), [`WebmCodec`](src/main/java/me/tamkungz/codecmedia/internal/video/webm/WebmCodec.java)) and probe info records.
- Added facade tests for MOV/WebM probe and strict validation in [`CodecMediaFacadeTest`](src/test/java/me/tamkungz/codecmedia/CodecMediaFacadeTest.java).

### Changed
- Extended probing and strict validation routing in [`StubCodecMediaEngine`](src/main/java/me/tamkungz/codecmedia/internal/StubCodecMediaEngine.java) to cover MOV and WebM.
- Updated project metadata URLs and branding references in [`pom.xml`](pom.xml) and [`README.md`](README.md).

### Fixed
- Fixed EBML element decoding in [`WebmParser`](src/main/java/me/tamkungz/codecmedia/internal/video/webm/WebmParser.java) so multi-byte element IDs (including `DefaultDuration`) are parsed with correct ID/size boundaries.
- Fixed MOV frame-rate extraction in [`MovParser`](src/main/java/me/tamkungz/codecmedia/internal/video/mov/MovParser.java) to use `mdhd` track timescale when interpreting `stts` sample delta.

## [1.0.1] - 2026-03-02

### Added
- Initial public release with probing, validation, metadata sidecar persistence, extraction workflow, playback simulation, and conversion routing.
