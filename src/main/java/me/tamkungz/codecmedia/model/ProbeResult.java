package me.tamkungz.codecmedia.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ProbeResult(
        Path input,
        String mimeType,
        String extension,
        MediaType mediaType,
        Long durationMillis,
        List<StreamInfo> streams,
        Map<String, String> tags
) {
}
