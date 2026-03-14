package me.tamkungz.codecmedia.options;

public record ValidationOptions(
        boolean strict,
        long maxBytes
) {

    /**
     * Default validation policy.
     *
     * <p>Uses non-strict parser mode and a maximum file size of 500 MiB.
     * Callers that require tighter limits should provide explicit options.
     */
    public static ValidationOptions defaults() {
        return new ValidationOptions(false, 500L * 1024 * 1024);
    }
}
