package me.tamkungz.codecmedia.internal.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.model.ConversionResult;

/**
 * Temporary stub converter for WAV <-> PCM routing.
 * <p>
 * This implementation does a byte-for-byte copy only. It does not perform
 * real PCM framing/transcoding.
 */
public final class WavPcmStubConverter implements MediaConverter {

    @Override
    public ConversionResult convert(ConversionRequest request) throws CodecMediaException {
        String source = request.sourceExtension();
        String target = request.targetExtension();

        boolean supportedPair = ("wav".equals(source) && "pcm".equals(target))
                || ("pcm".equals(source) && "wav".equals(target));
        if (!supportedPair) {
            throw new CodecMediaException(
                    "audio->audio transcoding is not implemented yet (supported stub pair: wav<->pcm only)"
            );
        }

        Path output = request.output();
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(output) && !request.options().overwrite()) {
                throw new CodecMediaException("Output already exists and overwrite is disabled: " + output);
            }
            Files.copy(request.input(), output, StandardCopyOption.REPLACE_EXISTING);
            return new ConversionResult(output, request.targetExtension(), false);
        } catch (IOException e) {
            throw new CodecMediaException("Failed to convert file: " + request.input(), e);
        }
    }
}

