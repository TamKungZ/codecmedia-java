package me.tamkungz.codecmedia.options;

public record AudioExtractOptions(
        String targetFormat,
        Integer bitrateKbps,
        Integer streamIndex
) {

    public static AudioExtractOptions defaults() {
        return new AudioExtractOptions("m4a", 192, 0);
    }
}
