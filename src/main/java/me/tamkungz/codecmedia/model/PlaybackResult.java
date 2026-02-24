package me.tamkungz.codecmedia.model;

public record PlaybackResult(
        boolean started,
        String backend,
        MediaType mediaType,
        String message
) {
}

