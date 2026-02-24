package me.tamkungz.codecmedia.options;

public record ConversionOptions(
        String targetFormat,
        String preset,
        boolean overwrite
) {

    public static ConversionOptions defaults(String targetFormat) {
        return new ConversionOptions(targetFormat, "balanced", false);
    }
}
