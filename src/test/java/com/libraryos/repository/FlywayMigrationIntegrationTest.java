package com.libraryos.repository;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationIntegrationTest {

    private static JdbcDataSource dataSource;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();
    }

    @Test
    @DisplayName("V1: Migration should seed exactly 5 core roles")
    void testCoreRolesSeeded() throws Exception {
        String sql = "SELECT COUNT(*) FROM roles";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("V1: Migration should seed exactly 30 granular permissions")
    void testPermissionsSeeded() throws Exception {
        String sql = "SELECT COUNT(*) FROM permissions";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(30, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("V1: Migration should seed initial Super Admin account 'admin'")
    void testSuperAdminSeeded() throws Exception {
        String sql = "SELECT u.id, u.username, u.full_name, u.status, r.code as role_code " +
                "FROM users u " +
                "JOIN user_roles ur ON u.id = ur.user_id " +
                "JOIN roles r ON ur.role_id = r.id " +
                "WHERE u.username = 'admin'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next(), "Expected user 'admin' to exist");
            assertEquals("admin", rs.getString("username"));
            assertEquals("System Administrator", rs.getString("full_name"));
            assertEquals("ACTIVE", rs.getString("status"));
            assertEquals("SUPER_ADMIN", rs.getString("role_code"));
        }
    }

    @Test
    @DisplayName("V2 & V6: Migration should seed 300 books, authors, categories, 11 shelves, and 7,934 physical copies")
    void testCatalogAndCopiesSeeded() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Check books
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM books");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(300, rs.getInt(1));
            }
            // Check physical copies
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM book_copies");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(7934, rs.getInt(1));
            }
            // Check shelves
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM shelves");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(11, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("V3: Migration should seed member types, borrowing policies, and sample members")
    void testMembersAndPoliciesSeeded() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Check member types
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM member_types");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt(1));
            }
            // Check borrowing policies
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM borrowing_policies");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(5, rs.getInt(1));
            }
            // Check members count (3 initial + 44 community friends)
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM members");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(47, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("V4 & V7: Migration should create circulation, reservation, and finance tables with friends data")
    void testCirculationAndFinanceSeeded() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Check loans table (1 seed + 29 friends loans = 30)
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM loans");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(30, rs.getInt(1));
            }
            // Check copy #1 is marked ISSUED
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM book_copies WHERE id = 1");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("ISSUED", rs.getString(1));
            }
            // Check reservations table (12 hold reservations)
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM reservations");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(12, rs.getInt(1));
            }
            // Check fines table (7 fines seeded)
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM fines");
                 ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(7, rs.getInt(1));
            }
        }
    }
}
