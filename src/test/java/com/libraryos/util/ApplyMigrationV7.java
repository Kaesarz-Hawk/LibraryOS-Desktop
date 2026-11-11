package com.libraryos.util;

import com.libraryos.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ApplyMigrationV7 {
    public static void main(String[] args) {
        System.out.println("Applying V7 to database...");
        DatabaseConfig.initialize();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to DB: " + (DatabaseConfig.isEmbedded() ? "Embedded H2" : "MySQL 8.0"));

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM members")) {
                if (rs.next()) System.out.println("Total Members: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM loans")) {
                if (rs.next()) System.out.println("Total Loans: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM fines")) {
                if (rs.next()) System.out.println("Total Fines: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM payments")) {
                if (rs.next()) System.out.println("Total Payments: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reservations")) {
                if (rs.next()) System.out.println("Total Reservations: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM suppliers")) {
                if (rs.next()) System.out.println("Total Suppliers: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM acquisitions")) {
                if (rs.next()) System.out.println("Total Acquisitions: " + rs.getInt(1));
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM reviews")) {
                if (rs.next()) System.out.println("Total Reviews: " + rs.getInt(1));
            }

            // Kawsar Hoque stats
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT m.full_name_en, " +
                    "  (SELECT COUNT(*) FROM loans WHERE member_id = m.id) as total_loans, " +
                    "  (SELECT COALESCE(SUM(amount_bdt),0) FROM payments WHERE member_id = m.id) as total_paid, " +
                    "  (SELECT COALESCE(SUM(original_amount_bdt - waived_amount_bdt - paid_amount_bdt),0) FROM fines WHERE member_id = m.id) as total_fines " +
                    "FROM members m WHERE m.id = 4")) {
                if (rs.next()) {
                    System.out.println("\n⭐ Patron: " + rs.getString("full_name_en") +
                            " | Loans: " + rs.getInt("total_loans") +
                            " | Paid: ৳ " + rs.getBigDecimal("total_paid") +
                            " | Unpaid Fines: ৳ " + rs.getBigDecimal("total_fines"));
                }
            }

            // Sarah Rahman stats
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT m.full_name_en, " +
                    "  (SELECT COUNT(*) FROM loans WHERE member_id = m.id) as total_loans, " +
                    "  (SELECT COALESCE(SUM(amount_bdt),0) FROM payments WHERE member_id = m.id) as total_paid, " +
                    "  (SELECT COALESCE(SUM(original_amount_bdt - waived_amount_bdt - paid_amount_bdt),0) FROM fines WHERE member_id = m.id) as total_fines " +
                    "FROM members m WHERE m.id = 5")) {
                if (rs.next()) {
                    System.out.println("⭐ Patron: " + rs.getString("full_name_en") +
                            " | Loans: " + rs.getInt("total_loans") +
                            " | Paid: ৳ " + rs.getBigDecimal("total_paid") +
                            " | Unpaid Fines: ৳ " + rs.getBigDecimal("total_fines"));
                }
            }

            // Risalat Bepari Rakiba stats
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT m.full_name_en, " +
                    "  (SELECT COUNT(*) FROM loans WHERE member_id = m.id) as total_loans, " +
                    "  (SELECT COUNT(*) FROM fines WHERE member_id = m.id) as fine_records, " +
                    "  (SELECT COALESCE(SUM(original_amount_bdt - waived_amount_bdt - paid_amount_bdt),0) FROM fines WHERE member_id = m.id) as total_fines " +
                    "FROM members m WHERE m.id = 6")) {
                if (rs.next()) {
                    System.out.println("⚠️ Patron: " + rs.getString("full_name_en") +
                            " | Loans: " + rs.getInt("total_loans") +
                            " | Fine Records: " + rs.getInt("fine_records") +
                            " | Pending Fines: ৳ " + rs.getBigDecimal("total_fines"));
                }
            }

            // Suppliers
            System.out.println("\n📦 Seeded Suppliers:");
            try (ResultSet rs = stmt.executeQuery("SELECT id, name, contact_person, phone FROM suppliers WHERE id >= 4")) {
                while (rs.next()) {
                    System.out.println(" - [" + rs.getLong("id") + "] " + rs.getString("name") +
                            " | Contact: " + rs.getString("contact_person") +
                            " | Phone: " + rs.getString("phone"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.shutdown();
        }
    }
}
