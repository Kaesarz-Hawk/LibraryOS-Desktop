package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.util.BarcodeUtil;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentAllocation;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.member.Member;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.List;

public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);    // Royal Blue #2563EB
    private static final Color DARK_SLATE = new Color(15, 23, 42);       // Slate 900 #0F172A
    private static final Color LIGHT_BG = new Color(248, 250, 252);      // Slate 50 #F8FAFC
    private static final Color BORDER_COLOR = new Color(226, 232, 240);  // Slate 200 #E2E8F0
    private static final Color SUCCESS_COLOR = new Color(22, 163, 74);   // Emerald #16A34A

    private Font fontHeader;
    private Font fontTitle;
    private Font fontBody;
    private Font fontBodyBold;
    private Font fontSmall;
    private Font fontSmallMuted;

    public PdfExportService() {
        initFonts();
    }

    private void initFonts() {
        // Register potential Bangla / Unicode fallback fonts on Windows
        try {
            String[] fontPaths = {
                    "C:/Windows/Fonts/Nirmala.ttf",
                    "C:/Windows/Fonts/NirmalaB.ttf",
                    "C:/Windows/Fonts/vrinda.ttf",
                    "C:/Windows/Fonts/kalpurush.ttf",
                    "C:/Windows/Fonts/segoeui.ttf"
            };
            for (String fp : fontPaths) {
                File f = new File(fp);
                if (f.exists()) {
                    FontFactory.register(fp);
                }
            }
        } catch (Exception e) {
            log.debug("Font registration notice: {}", e.getMessage());
        }

        fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, DARK_SLATE);
        fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY_COLOR);
        fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10, DARK_SLATE);
        fontBodyBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_SLATE);
        fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, DARK_SLATE);
        fontSmallMuted = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));
    }

    /**
     * Generates a printable PDF sheet containing Code-128 spine barcodes for book copies.
     */
    public void generateSpineLabelsPdf(List<BookCopy> copies, OutputStream out) {
        PermissionGuard.requirePermission("BARCODE_GENERATE");
        Document doc = new Document(PageSize.A4, 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph header = new Paragraph("LibraryOS — Book Spine Barcode Labels", fontHeader);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(15);
            doc.add(header);

            // 3-column label table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            for (BookCopy copy : copies) {
                PdfPCell labelCell = new PdfPCell();
                labelCell.setPadding(8);
                labelCell.setBorder(Rectangle.BOX);
                labelCell.setBorderColor(BORDER_COLOR);
                labelCell.setBackgroundColor(Color.WHITE);

                Paragraph instP = new Paragraph("LIBRARY ACCESSION", fontSmallMuted);
                instP.setAlignment(Element.ALIGN_CENTER);

                String title = copy.getBookTitle() != null ? copy.getBookTitle() : "Book #" + copy.getBookId();
                if (title.length() > 32) title = title.substring(0, 29) + "...";
                Paragraph titleP = new Paragraph(title, fontSmall);
                titleP.setAlignment(Element.ALIGN_CENTER);

                Paragraph accP = new Paragraph("Acc: " + (copy.getAccessionNumber() != null ? copy.getAccessionNumber() : "—")
                        + " | Shelf: " + (copy.getShelfLocation() != null ? copy.getShelfLocation() : "—"), fontSmall);
                accP.setAlignment(Element.ALIGN_CENTER);

                // Barcode image
                BufferedImage barcodeImg = BarcodeUtil.generateCode128Barcode(copy.getBarcode(), 240, 50);
                byte[] barcodeBytes = BarcodeUtil.toPngByteArray(barcodeImg);
                Image img = Image.getInstance(barcodeBytes);
                img.setAlignment(Element.ALIGN_CENTER);
                img.scalePercent(60);

                Paragraph codeP = new Paragraph(copy.getBarcode(), fontBodyBold);
                codeP.setAlignment(Element.ALIGN_CENTER);

                labelCell.addElement(instP);
                labelCell.addElement(titleP);
                labelCell.addElement(accP);
                labelCell.addElement(img);
                labelCell.addElement(codeP);

                table.addCell(labelCell);
            }

            // Fill remaining cells in row if needed
            table.completeRow();
            doc.add(table);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate spine labels PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * Generates a printable Member Library ID Card in PDF.
     */
    public void generateMemberIdCardPdf(Member member, OutputStream out) {
        PermissionGuard.requirePermission("MEMBER_VIEW");
        Document doc = new Document(new Rectangle(260, 165), 10, 10, 10, 10); // Standard Card size
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            PdfPTable card = new PdfPTable(2);
            card.setWidthPercentage(100);
            card.setWidths(new float[]{65, 35});

            // Left Side: Member Details
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(4);

            Paragraph org = new Paragraph("LIBRARY IDENTITY CARD", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR));
            Paragraph name = new Paragraph(member.getDisplayName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_SLATE));
            Paragraph id = new Paragraph("Card #: " + member.getMembershipNumber(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, DARK_SLATE));
            Paragraph type = new Paragraph("Type: " + (member.getMemberTypeName() != null ? member.getMemberTypeName() : "General"), fontSmall);
            Paragraph instId = new Paragraph("Inst ID: " + (member.getInstitutionalId() != null ? member.getInstitutionalId() : "—"), fontSmall);
            Paragraph exp = new Paragraph("Valid Thru: " + (member.getMembershipExpiryDate() != null ? DateTimeUtil.formatDate(member.getMembershipExpiryDate()) : "Permanent"), fontSmallMuted);

            leftCell.addElement(org);
            leftCell.addElement(name);
            leftCell.addElement(id);
            leftCell.addElement(type);
            leftCell.addElement(instId);
            leftCell.addElement(exp);

            // Right Side: QR Code
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setPadding(4);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            BufferedImage qrImg = BarcodeUtil.generateQrCode("MEMBER:" + member.getMembershipNumber(), 120);
            byte[] qrBytes = BarcodeUtil.toPngByteArray(qrImg);
            Image qr = Image.getInstance(qrBytes);
            qr.scalePercent(65);
            qr.setAlignment(Element.ALIGN_CENTER);

            Paragraph qrLabel = new Paragraph(member.getMembershipNumber(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, DARK_SLATE));
            qrLabel.setAlignment(Element.ALIGN_CENTER);

            rightCell.addElement(qr);
            rightCell.addElement(qrLabel);

            card.addCell(leftCell);
            card.addCell(rightCell);

            doc.add(card);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate member ID card PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * Generates an official payment receipt / cash voucher in PDF.
     */
    public void generatePaymentReceiptPdf(Payment payment, List<PaymentAllocation> allocations, OutputStream out) {
        PermissionGuard.requirePermission("FINE_VIEW");
        Document doc = new Document(PageSize.A5, 24, 24, 24, 24);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Header Banner
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{70, 30});

            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            Paragraph h1 = new Paragraph("LibraryOS", fontHeader);
            Paragraph h2 = new Paragraph("OFFICIAL PAYMENT RECEIPT / অর্থ আদায় রসিদ", fontTitle);
            titleCell.addElement(h1);
            titleCell.addElement(h2);

            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            BufferedImage qrImg = BarcodeUtil.generateQrCode("RECEIPT:" + payment.getReceiptNumber(), 100);
            Image qr = Image.getInstance(BarcodeUtil.toPngByteArray(qrImg));
            qr.scalePercent(55);
            qr.setAlignment(Element.ALIGN_RIGHT);
            qrCell.addElement(qr);

            headerTable.addCell(titleCell);
            headerTable.addCell(qrCell);
            doc.add(headerTable);

            // Receipt Metadata Grid
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingBefore(12);
            metaTable.setSpacingAfter(14);

            addMetaRow(metaTable, "Receipt Number:", payment.getReceiptNumber(), true);
            addMetaRow(metaTable, "Date & Time:", payment.getCreatedAt() != null ? DateTimeUtil.formatDateTime(payment.getCreatedAt()) : "—", false);
            addMetaRow(metaTable, "Member Name:", (payment.getMemberName() != null ? payment.getMemberName() : "—") + " (" + payment.getMembershipNumber() + ")", false);
            addMetaRow(metaTable, "Payment Method:", payment.getPaymentMethod().name() + (payment.getTransactionReference() != null ? " (TrxID: " + payment.getTransactionReference() + ")" : ""), false);
            addMetaRow(metaTable, "Collected By:", payment.getReceivedByUsername() != null ? payment.getReceivedByUsername() : "Staff #" + payment.getReceivedByUserId(), false);
            doc.add(metaTable);

            // Allocations Table
            Paragraph allocTitle = new Paragraph("Allocated Fine Breakdown", fontBodyBold);
            allocTitle.setSpacingAfter(6);
            doc.add(allocTitle);

            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{20, 55, 25});
            itemsTable.setSpacingAfter(16);

            addHeaderCell(itemsTable, "Fine #");
            addHeaderCell(itemsTable, "Description / Details");
            addHeaderCell(itemsTable, "Allocated (৳)");

            if (allocations != null && !allocations.isEmpty()) {
                for (PaymentAllocation a : allocations) {
                    addBodyCell(itemsTable, String.valueOf(a.getFineId()), Element.ALIGN_CENTER);
                    addBodyCell(itemsTable, "Fine Liability Payment", Element.ALIGN_LEFT);
                    addBodyCell(itemsTable, "৳ " + a.getAllocatedAmountBdt().toPlainString(), Element.ALIGN_RIGHT);
                }
            } else {
                addBodyCell(itemsTable, "—", Element.ALIGN_CENTER);
                addBodyCell(itemsTable, "Consolidated payment allocation", Element.ALIGN_LEFT);
                addBodyCell(itemsTable, "৳ " + payment.getAmount().toPlainString(), Element.ALIGN_RIGHT);
            }
            doc.add(itemsTable);

            // Total Card
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{65, 35});
            totalTable.setSpacingAfter(24);

            PdfPCell wordsCell = new PdfPCell(new Phrase("Status: " + (payment.isReversed() ? "REVERSED" : "COMPLETED / PAID IN FULL"), fontBodyBold));
            wordsCell.setBorder(Rectangle.NO_BORDER);
            wordsCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPCell sumCell = new PdfPCell();
            sumCell.setBorder(Rectangle.BOX);
            sumCell.setBackgroundColor(LIGHT_BG);
            sumCell.setPadding(8);
            Paragraph sumP = new Paragraph("TOTAL COLLECTED\n৳ " + payment.getAmount().toPlainString(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, SUCCESS_COLOR));
            sumP.setAlignment(Element.ALIGN_RIGHT);
            sumCell.addElement(sumP);

            totalTable.addCell(wordsCell);
            totalTable.addCell(sumCell);
            doc.add(totalTable);

            // Signatures
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingBefore(30);

            PdfPCell sig1 = new PdfPCell(new Paragraph("__________________________\nMember / Payee Signature", fontSmallMuted));
            sig1.setBorder(Rectangle.NO_BORDER);
            sig1.setHorizontalAlignment(Element.ALIGN_LEFT);

            PdfPCell sig2 = new PdfPCell(new Paragraph("__________________________\nAuthorized Librarian / Seal", fontSmallMuted));
            sig2.setBorder(Rectangle.NO_BORDER);
            sig2.setHorizontalAlignment(Element.ALIGN_RIGHT);

            sigTable.addCell(sig1);
            sigTable.addCell(sig2);
            doc.add(sigTable);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payment receipt PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    /**
     * Generates a generic tabular report in PDF (overdue items, catalog inventory, fine collections, circulation trends).
     */
    public void generateTabularReportPdf(String title, String subtitle, List<String> headers,
                                         List<List<String>> rows, List<String> totals, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        Document doc = new Document(PageSize.A4.rotate(), 20, 20, 20, 20); // Landscape
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph t = new Paragraph("LibraryOS — " + title, fontHeader);
            t.setAlignment(Element.ALIGN_CENTER);
            Paragraph sub = new Paragraph(subtitle + " • Generated on " + DateTimeUtil.formatDateTime(java.time.LocalDateTime.now()), fontSmallMuted);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(14);
            doc.add(t);
            doc.add(sub);

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);

            for (String h : headers) {
                addHeaderCell(table, h);
            }

            for (List<String> row : rows) {
                for (String val : row) {
                    addBodyCell(table, val != null ? val : "—", Element.ALIGN_LEFT);
                }
            }

            if (totals != null && totals.size() == headers.size()) {
                for (String tot : totals) {
                    PdfPCell cell = new PdfPCell(new Phrase(tot != null ? tot : "", fontBodyBold));
                    cell.setBackgroundColor(LIGHT_BG);
                    cell.setPadding(6);
                    cell.setBorderColor(BORDER_COLOR);
                    table.addCell(cell);
                }
            }

            doc.add(table);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate tabular report PDF", e);
        } finally {
            if (doc.isOpen()) doc.close();
        }
    }

    private void addMetaRow(PdfPTable table, String label, String value, boolean isBold) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fontBodyBold));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3);

        PdfPCell c2 = new PdfPCell(new Phrase(value, isBold ? fontBodyBold : fontBody));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3);

        table.addCell(c1);
        table.addCell(c2);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fontSmall));
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }
}

