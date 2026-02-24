package me.tamkungz.codecmedia.internal.convert;

import me.tamkungz.codecmedia.CodecMediaException;
import me.tamkungz.codecmedia.model.ConversionResult;

public final class DefaultConversionHub implements ConversionHub {

    private final MediaConverter passthroughConverter = new SameFormatCopyConverter();
    private final MediaConverter wavPcmStubConverter = new WavPcmStubConverter();
    private final MediaConverter videoToAudioConverter = new UnsupportedRouteConverter(
            "video->audio conversion is not implemented yet (planned conversion hub path)"
    );
    private final MediaConverter audioToImageConverter = new UnsupportedRouteConverter(
            "audio->image (album cover) conversion is not implemented yet (planned conversion hub path)"
    );
    private final MediaConverter videoToVideoConverter = new UnsupportedRouteConverter(
            "video->video conversion is not implemented yet (planned conversion hub path)"
    );
    private final MediaConverter audioToAudioTranscodeConverter = new UnsupportedRouteConverter(
            "audio->audio transcoding is not implemented yet (planned conversion hub path)"
    );
    private final MediaConverter imageToImageTranscodeConverter = new UnsupportedRouteConverter(
            "image->image transcoding is not implemented yet (planned conversion hub path)"
    );

    @Override
    public ConversionResult convert(ConversionRequest request) throws CodecMediaException {
        if (request.sourceExtension().equals(request.targetExtension())) {
            return passthroughConverter.convert(request);
        }

        ConversionRoute route = ConversionRouteResolver.resolve(request.sourceMediaType(), request.targetMediaType());
        return switch (route) {
            case VIDEO_TO_AUDIO -> videoToAudioConverter.convert(request);
            case AUDIO_TO_IMAGE -> audioToImageConverter.convert(request);
            case VIDEO_TO_VIDEO -> videoToVideoConverter.convert(request);
            case AUDIO_TO_AUDIO -> {
                boolean wavPcmPair = ("wav".equals(request.sourceExtension()) && "pcm".equals(request.targetExtension()))
                        || ("pcm".equals(request.sourceExtension()) && "wav".equals(request.targetExtension()));
                if (wavPcmPair) {
                    yield wavPcmStubConverter.convert(request);
                }
                yield audioToAudioTranscodeConverter.convert(request);
            }
            case IMAGE_TO_IMAGE -> imageToImageTranscodeConverter.convert(request);
            case UNSUPPORTED -> throw new CodecMediaException(
                    "Unsupported conversion route: " + request.sourceMediaType() + " -> " + request.targetMediaType()
            );
        };
    }
}

