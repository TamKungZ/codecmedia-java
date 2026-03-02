# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
