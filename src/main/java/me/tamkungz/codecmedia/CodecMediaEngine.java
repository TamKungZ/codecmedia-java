package me.tamkungz.codecmedia;

import java.nio.file.Path;
import me.tamkungz.codecmedia.model.ConversionResult;
import me.tamkungz.codecmedia.model.ExtractionResult;
import me.tamkungz.codecmedia.model.Metadata;
import me.tamkungz.codecmedia.model.ProbeResult;
import me.tamkungz.codecmedia.model.ValidationResult;
import me.tamkungz.codecmedia.options.AudioExtractOptions;
import me.tamkungz.codecmedia.options.ConversionOptions;
import me.tamkungz.codecmedia.options.ValidationOptions;

public interface CodecMediaEngine {

    ProbeResult probe(Path input) throws CodecMediaException;

    Metadata readMetadata(Path input) throws CodecMediaException;

    void writeMetadata(Path input, Metadata metadata) throws CodecMediaException;

    ExtractionResult extractAudio(Path input, Path outputDir, AudioExtractOptions options) throws CodecMediaException;

    ConversionResult convert(Path input, Path output, ConversionOptions options) throws CodecMediaException;

    ValidationResult validate(Path input, ValidationOptions options) throws CodecMediaException;
}
