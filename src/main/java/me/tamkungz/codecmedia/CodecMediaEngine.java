package me.tamkungz.codecmedia;

import java.nio.file.Path;
import me.tamkungz.codecmedia.model.ConversionResult;
import me.tamkungz.codecmedia.model.ExtractionResult;
import me.tamkungz.codecmedia.model.Metadata;
import me.tamkungz.codecmedia.model.PlaybackResult;
import me.tamkungz.codecmedia.model.ProbeResult;
import me.tamkungz.codecmedia.model.ValidationResult;
import me.tamkungz.codecmedia.options.AudioExtractOptions;
import me.tamkungz.codecmedia.options.ConversionOptions;
import me.tamkungz.codecmedia.options.PlaybackOptions;
import me.tamkungz.codecmedia.options.ValidationOptions;

/**
 * Core media engine contract used by {@link CodecMedia}.
 * <p>
 * The current default implementation focuses on practical probing/validation workflows and
 * light-weight conversion routing. For richer embedded metadata (for example MP3 album cover/APIC),
 * callers should treat {@link #probe(Path)} output as technical media info rather than full tag extraction.
 */
public interface CodecMediaEngine {

    /**
     * Convenience alias of {@link #probe(Path)}.
     *
     * @param input media file path
     * @return probe result describing detected media characteristics
     * @throws CodecMediaException when probing fails
     */
    ProbeResult get(Path input) throws CodecMediaException;

    /**
     * Detects media format and returns technical stream/container information.
     *
     * @param input media file path
     * @return probe result containing mime, extension, media type, streams, and basic tags
     * @throws CodecMediaException when the file is missing or parsing fails
     */
    ProbeResult probe(Path input) throws CodecMediaException;

    /**
     * Reads metadata associated with the file.
     * <p>
     * In the default stub implementation this reads sidecar metadata plus base probe fields,
     * not full embedded tag catalogs for every format.
     *
     * @param input media file path
     * @return metadata entries
     * @throws CodecMediaException when reading fails
     */
    Metadata readMetadata(Path input) throws CodecMediaException;

    /**
     * Writes metadata associated with the file.
     * <p>
     * In the default stub implementation this writes a sidecar properties file.
     *
     * @param input media file path
     * @param metadata metadata entries to persist
     * @throws CodecMediaException when validation or writing fails
     */
    void writeMetadata(Path input, Metadata metadata) throws CodecMediaException;

    /**
     * Extracts audio from an input media file into the given output directory.
     *
     * @param input source media file
     * @param outputDir target directory for extracted output
     * @param options extraction options; implementation defaults may be used when {@code null}
     * @return extraction result describing output file and format
     * @throws CodecMediaException when extraction is unsupported or fails
     */
    ExtractionResult extractAudio(Path input, Path outputDir, AudioExtractOptions options) throws CodecMediaException;

    /**
     * Converts media according to the requested options.
     *
     * @param input source media file
     * @param output output media file path
     * @param options conversion options; implementation defaults may be used when {@code null}
     * @return conversion result
     * @throws CodecMediaException when route is unsupported or conversion fails
     */
    ConversionResult convert(Path input, Path output, ConversionOptions options) throws CodecMediaException;

    /**
     * Starts playback/viewing for supported media.
     *
     * @param input source media file
     * @param options playback options controlling dry-run and external app behavior
     * @return playback result including backend and started status
     * @throws CodecMediaException when playback cannot be started
     */
    PlaybackResult play(Path input, PlaybackOptions options) throws CodecMediaException;

    /**
     * Validates file existence and optional strict format constraints.
     *
     * @param input media file path
     * @param options validation options; implementation defaults may be used when {@code null}
     * @return validation result with warnings/errors
     */
    ValidationResult validate(Path input, ValidationOptions options) throws CodecMediaException;
}
