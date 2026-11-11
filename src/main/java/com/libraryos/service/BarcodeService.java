package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.util.BarcodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

public class BarcodeService {

    private static final Logger log = LoggerFactory.getLogger(BarcodeService.class);

    private final AuditService auditService;

    public BarcodeService(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Generates a Code-128 barcode image for an accession or physical copy barcode.
     */
    public BufferedImage generateCopyBarcode(String barcode, int width, int height) {
        PermissionGuard.requirePermission("BARCODE_GENERATE");
        log.debug("Generating Code-128 barcode for copy: {}", barcode);
        return BarcodeUtil.generateCode128Barcode(barcode, width, height);
    }

    /**
     * Generates a Code-128 barcode PNG byte array for direct PDF embedding.
     */
    public byte[] generateCopyBarcodePng(String barcode, int width, int height) {
        BufferedImage img = generateCopyBarcode(barcode, width, height);
        return BarcodeUtil.toPngByteArray(img);
    }

    /**
     * Generates a QR code image for a member ID card.
     */
    public BufferedImage generateMemberQrCode(String membershipNumber, int size) {
        PermissionGuard.requirePermission("BARCODE_GENERATE");
        log.debug("Generating QR code for member card: {}", membershipNumber);
        return BarcodeUtil.generateQrCode("MEMBER:" + membershipNumber, size);
    }

    /**
     * Generates a QR code PNG byte array for a member card.
     */
    public byte[] generateMemberQrCodePng(String membershipNumber, int size) {
        BufferedImage img = generateMemberQrCode(membershipNumber, size);
        return BarcodeUtil.toPngByteArray(img);
    }

    /**
     * Generates a QR code for a payment receipt verification.
     */
    public BufferedImage generateReceiptQrCode(String receiptNumber, int size) {
        PermissionGuard.requirePermission("BARCODE_GENERATE");
        log.debug("Generating QR code for receipt: {}", receiptNumber);
        return BarcodeUtil.generateQrCode("RECEIPT:" + receiptNumber, size);
    }

    /**
     * Generates a QR code PNG byte array for a payment receipt.
     */
    public byte[] generateReceiptQrCodePng(String receiptNumber, int size) {
        BufferedImage img = generateReceiptQrCode(receiptNumber, size);
        return BarcodeUtil.toPngByteArray(img);
    }
}

