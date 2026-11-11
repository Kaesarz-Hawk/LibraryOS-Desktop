package com.libraryos.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class BarcodeUtilTest {

    @Test
    @DisplayName("Should generate valid Code-128 barcode and decode successfully")
    void testCode128BarcodeGenerationAndDecoding() {
        String testBarcode = "LIB-2026-000101";
        BufferedImage img = BarcodeUtil.generateCode128Barcode(testBarcode, 300, 100);

        assertNotNull(img);
        assertEquals(300, img.getWidth());
        assertEquals(100, img.getHeight());

        byte[] pngBytes = BarcodeUtil.toPngByteArray(img);
        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);

        String decoded = BarcodeUtil.decodeImage(img);
        assertEquals(testBarcode, decoded);
    }

    @Test
    @DisplayName("Should generate valid QR Code and decode successfully")
    void testQrCodeGenerationAndDecoding() {
        String testPayload = "MEMBER:LIB-2026-0001";
        BufferedImage img = BarcodeUtil.generateQrCode(testPayload, 200);

        assertNotNull(img);
        assertEquals(200, img.getWidth());
        assertEquals(200, img.getHeight());

        byte[] pngBytes = BarcodeUtil.toPngByteArray(img);
        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);

        String decoded = BarcodeUtil.decodeImage(img);
        assertEquals(testPayload, decoded);
    }

    @Test
    @DisplayName("Should reject blank or empty barcode inputs")
    void testInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> BarcodeUtil.generateCode128Barcode("", 200, 100));
        assertThrows(IllegalArgumentException.class, () -> BarcodeUtil.generateQrCode(null, 200));
    }
}

