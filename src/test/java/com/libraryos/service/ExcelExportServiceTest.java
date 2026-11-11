package com.libraryos.service;

import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExcelExportServiceTest {

    private ExcelExportService excelService;

    @BeforeEach
    void setUp() {
        excelService = new ExcelExportService();
        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("REPORT_EXPORT"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should export valid Excel .xlsx workbook with correct headers and rows")
    void testExportToExcel() throws Exception {
        List<String> headers = List.of("Book ID", "Title", "Copies", "Price (BDT)");
        List<List<String>> data = List.of(
                List.of("1", "Clean Code", "3", "৳ 650.00"),
                List.of("2", "Design Patterns (ডিজাইন প্যাটার্নস)", "5", "৳ 850.00")
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        excelService.exportToExcel("Catalog_Dump", headers, data, baos);

        byte[] xlsxBytes = baos.toByteArray();
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 1000);

        // Read workbook back with Apache POI to verify structural validity
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = wb.getSheet("Catalog_Dump");
            assertNotNull(sheet);
            assertEquals(3, sheet.getPhysicalNumberOfRows()); // Header + 2 data rows

            Row hRow = sheet.getRow(0);
            assertEquals("Book ID", hRow.getCell(0).getStringCellValue());
            assertEquals("Title", hRow.getCell(1).getStringCellValue());

            Row r1 = sheet.getRow(1);
            assertEquals("1", r1.getCell(0).getStringCellValue());
            assertEquals("Clean Code", r1.getCell(1).getStringCellValue());

            Row r2 = sheet.getRow(2);
            assertEquals("Design Patterns (ডিজাইন প্যাটার্নস)", r2.getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Should export valid UTF-8 CSV with BOM for Excel Bangla support")
    void testExportToCsv() {
        List<String> headers = List.of("ID", "Name", "City");
        List<List<String>> data = List.of(
                List.of("1", "Tariqul, Islam", "Dhaka"),
                List.of("2", "আনিসুল হক", "Chittagong")
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        excelService.exportToCsv(headers, data, baos);

        byte[] csvBytes = baos.toByteArray();
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 10);

        // Verify UTF-8 BOM: 0xEF, 0xBB, 0xBF
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String content = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"Tariqul, Islam\"")); // quoted comma
        assertTrue(content.contains("আনিসুল হক")); // Bangla unicode preserved
    }
}

