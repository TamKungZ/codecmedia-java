package me.tamkungz.codecmedia.internal.video.mp4;

public record Mp4ProbeInfo(
        Long durationMillis,
        Integer width,
        Integer height,
        String majorBrand
) {
}

