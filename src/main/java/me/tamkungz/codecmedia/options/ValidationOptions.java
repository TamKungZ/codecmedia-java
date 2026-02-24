package me.tamkungz.codecmedia.options;

public record ValidationOptions(
        boolean strict,
        long maxBytes
) {

    public static ValidationOptions defaults() {
        return new ValidationOptions(false, 500L * 1024 * 1024);
    }
}
