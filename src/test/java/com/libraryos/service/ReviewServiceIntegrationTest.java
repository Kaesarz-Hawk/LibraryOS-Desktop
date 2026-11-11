package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.interaction.Review;
import com.libraryos.repository.impl.JdbcAuditLogRepository;
import com.libraryos.repository.impl.JdbcReviewRepository;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReviewServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private ReviewService reviewService;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_review_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        DatabaseConfig.setDataSource(dataSource);
    }

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();

        JdbcReviewRepository reviewRepo = new JdbcReviewRepository();
        JdbcAuditLogRepository auditRepo = new JdbcAuditLogRepository();
        AuditService auditService = new AuditService(auditRepo);
        reviewService = new ReviewService(reviewRepo, auditService);

        UserSession adminSession = new UserSession(
                1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("BOOK_UPDATE")
        );
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    void testSubmitReviewAndCalculateRatings() {
        Review r1 = reviewService.submitReview(1L, 1L, 5, "Magnificent Poetry", "Rabindranath Tagore's Gitanjali is world heritage.");
        assertNotNull(r1.getId());
        assertEquals("★★★★★", r1.getStarsDisplay());

        Review r2 = reviewService.submitReview(1L, 2L, 3, "Good translation", "Rich imagery and spiritual depth.");
        assertNotNull(r2.getId());

        List<Review> approved = reviewService.getApprovedReviewsForBook(1L);
        assertEquals(2, approved.size());

        double avg = reviewService.getAverageRating(1L);
        assertEquals(4.0, avg, 0.01);

        int count = reviewService.getReviewCount(1L);
        assertEquals(2, count);

        // Moderation
        boolean mod = reviewService.moderateReview(r2.getId(), false, true);
        assertTrue(mod);

        assertEquals(1, reviewService.getReviewCount(1L));
        assertEquals(5.0, reviewService.getAverageRating(1L), 0.01);
    }
}
