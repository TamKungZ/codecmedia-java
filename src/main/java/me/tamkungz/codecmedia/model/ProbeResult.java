package me.tamkungz.codecmedia.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ProbeResult(
        Path input,
        String mimeType,
        String extension,
        MediaType mediaType,
        Long durationMillis,
        List<StreamInfo> streams,
        Map<String, String> tags
) {

    /**
     * Returns the codec of the first stream, when available.
     *
     * <p>This helper avoids direct {@code streams().get(0)} access for callers
     * and safely handles cases where probe results contain no streams.
     */
    public Optional<String> primaryCodec() {
        if (streams == null || streams.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(streams.get(0).codec());
    }
}
