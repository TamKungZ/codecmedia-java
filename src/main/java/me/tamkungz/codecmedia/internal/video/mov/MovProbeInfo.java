package me.tamkungz.codecmedia.internal.video.mov;

public record MovProbeInfo(
        Long durationMillis,
        Integer width,
        Integer height,
        String majorBrand,
        String videoCodec,
        String audioCodec,
        Integer sampleRate,
        Integer channels,
        Double frameRate
) {
}

