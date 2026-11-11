package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExcelExportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportService.class);

    /**
     * Exports tabular data to a professionally styled Microsoft Excel (.xlsx) spreadsheet.
     */
    public void exportToExcel(String sheetName, List<String> headers, List<List<String>> rows, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName != null && !sheetName.isBlank() ? sheetName : "Report");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(headerStyle);

            // Data Row Styles
            CellStyle dataStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font dataFont = workbook.createFont();
            dataFont.setFontHeightInPoints((short) 10);
            dataStyle.setFont(dataFont);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(dataStyle);

            CellStyle altDataStyle = workbook.createCellStyle();
            altDataStyle.setFont(dataFont);
            altDataStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            altDataStyle.setFillPattern(FillPatternType.NO_FILL);
            altDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorders(altDataStyle);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Populate Data Rows
            int rowNum = 1;
            for (List<String> rowData : rows) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(19);
                CellStyle currentStyle = (rowNum % 2 == 0) ? altDataStyle : dataStyle;

                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    String val = rowData.get(col);
                    cell.setCellValue(val != null ? val : "");
                    cell.setCellStyle(currentStyle);
                }
            }

            // Auto-size columns with a reasonable max limit
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 3000) {
                    sheet.setColumnWidth(i, 3000);
                } else if (currentWidth > 12000) {
                    sheet.setColumnWidth(i, 12000);
                }
            }

            workbook.write(out);
            log.info("Successfully exported {} rows to Excel sheet '{}'", rows.size(), sheetName);

        } catch (Exception e) {
            throw new RuntimeException("Failed to export Excel spreadsheet", e);
        }
    }

    /**
     * Exports tabular data to UTF-8 CSV with Byte Order Mark (BOM) for Excel Bangla support.
     */
    public void exportToCsv(List<String> headers, List<List<String>> rows, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");

        try {
            // Write UTF-8 BOM so Microsoft Excel correctly renders Bangla unicode characters
            out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

            // Write Headers
            for (int i = 0; i < headers.size(); i++) {
                writer.write(escapeCsv(headers.get(i)));
                if (i < headers.size() - 1) writer.write(",");
            }
            writer.newLine();

            // Write Data
            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    String val = row.get(i);
                    writer.write(escapeCsv(val != null ? val : ""));
                    if (i < row.size() - 1) writer.write(",");
                }
                writer.newLine();
            }

            writer.flush();
            log.info("Successfully exported {} rows to CSV", rows.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV file", e);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }
}

