package me.tamkungz.codecmedia.internal.image.heif;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import me.tamkungz.codecmedia.CodecMediaException;

public final class HeifCodec {

    private HeifCodec() {
    }

    public static BufferedImage decode(Path input) throws CodecMediaException {
        try {
            BufferedImage image = ImageIO.read(input.toFile());
            if (image == null) {
                throw new CodecMediaException("Unable to decode HEIF/HEIC: " + input);
            }
            validateDecodedImage(image, input);
            return image;
        } catch (IOException e) {
            throw new CodecMediaException("Failed to decode HEIF/HEIC: " + input, e);
        }
    }

    public static void encode(BufferedImage image, Path output, String targetExtension) throws CodecMediaException {
        String formatName = normalizeTargetExtension(targetExtension);
        try {
            boolean written = ImageIO.write(image, formatName, output.toFile());
            if (!written) {
                throw new CodecMediaException("No " + formatName.toUpperCase(java.util.Locale.ROOT)
                        + " writer available in ImageIO runtime");
            }
        } catch (IOException e) {
            throw new CodecMediaException("Failed to encode HEIF/HEIC: " + output, e);
        }
    }

    private static String normalizeTargetExtension(String extension) throws CodecMediaException {
        if (extension == null) {
            throw new CodecMediaException("HEIF/HEIC target extension is required");
        }
        String value = extension.trim().toLowerCase(java.util.Locale.ROOT);
        if (value.startsWith(".")) {
            value = value.substring(1);
        }
        if ("heif".equals(value)) {
            return "heif";
        }
        if ("heic".equals(value)) {
            return "heic";
        }
        throw new CodecMediaException("Unsupported HEIF target extension: " + extension);
    }

    private static void validateDecodedImage(BufferedImage image, Path input) throws CodecMediaException {
        if (image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new CodecMediaException("Decoded HEIF/HEIC has invalid dimensions: " + input);
        }
        if (image.getColorModel() == null || image.getColorModel().getPixelSize() <= 0) {
            throw new CodecMediaException("Decoded HEIF/HEIC has invalid bit depth: " + input);
        }
        if (image.getRaster() == null || image.getRaster().getNumBands() <= 0) {
            throw new CodecMediaException("Decoded HEIF/HEIC has invalid pixel channels: " + input);
        }
    }
}

