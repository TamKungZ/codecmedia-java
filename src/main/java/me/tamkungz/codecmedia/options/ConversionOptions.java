package me.tamkungz.codecmedia.options;

public record ConversionOptions(
        String targetFormat,
        String preset,
        boolean overwrite
) {

    public static ConversionOptions defaults() {
        return defaults("m4a");
    }

    public static ConversionOptions defaults(String targetFormat) {
        String effective = (targetFormat == null || targetFormat.isBlank()) ? "m4a" : targetFormat;
        return new ConversionOptions(effective, "balanced", false);
    }
}
