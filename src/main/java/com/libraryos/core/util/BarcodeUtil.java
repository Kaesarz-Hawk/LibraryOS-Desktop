package com.libraryos.core.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * High-performance barcode (Code 128) and QR Code generator and decoder powered by ZXing.
 */
public final class BarcodeUtil {

    private BarcodeUtil() {
    }

    /**
     * Generates a 1D Code-128 barcode image (standard for library accession and book inventory labels).
     *
     * @param text   Barcode text (e.g., accession number or unique copy barcode)
     * @param width  Target image width in pixels (e.g. 300)
     * @param height Target image height in pixels (e.g. 100)
     * @return BufferedImage containing the rendered barcode
     */
    public static BufferedImage generateCode128Barcode(String text, int width, int height) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Barcode text cannot be empty");
        }
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(text.trim(), BarcodeFormat.CODE_128, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Code-128 barcode for: " + text, e);
        }
    }

    /**
     * Generates a 2D QR Code image (used for member ID cards, receipts, and digital access).
     *
     * @param content Text or URL payload
     * @param size    Width and height in pixels (e.g. 200)
     * @return BufferedImage containing the rendered QR code
     */
    public static BufferedImage generateQrCode(String content, int size) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("QR content cannot be empty");
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content.trim(), BarcodeFormat.QR_CODE, size, size);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code for content: " + content, e);
        }
    }

    /**
     * Converts a BufferedImage to a PNG byte array for PDF embedding or file storage.
     */
    public static byte[] toPngByteArray(BufferedImage image) {
        if (image == null) return new byte[0];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert image to PNG byte array", e);
        }
    }

    /**
     * Decodes a barcode or QR code from a BufferedImage.
     */
    public static String decodeImage(BufferedImage image) {
        if (image == null) return null;
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (Exception e) {
            return null; // Return null if not decodable
        }
    }

    /**
     * Decodes a barcode or QR code from a byte array.
     */
    public static String decodeByteArray(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            return decodeImage(image);
        } catch (IOException e) {
            return null;
        }
    }
}

