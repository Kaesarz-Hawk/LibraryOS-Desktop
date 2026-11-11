package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.model.interaction.Notification;
import com.libraryos.repository.impl.JdbcNotificationRepository;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private NotificationService notificationService;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_notif_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
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

        JdbcNotificationRepository notifRepo = new JdbcNotificationRepository();
        notificationService = new NotificationService(notifRepo);
    }

    @Test
    void testSendAndMarkNotifications() {
        Long userId = 1L;

        Notification n1 = notificationService.sendNotification(userId, "Overdue Notice", "Book copy BC100001 is overdue.", "OVERDUE");
        Notification n2 = notificationService.sendNotification(userId, "Hold Ready", "Your reserved book is ready for pickup.", "HOLD_PICKUP");

        assertNotNull(n1.getId());
        assertNotNull(n2.getId());

        List<Notification> unread = notificationService.getUnreadNotifications(userId);
        assertEquals(2, unread.size());

        boolean markedOne = notificationService.markAsRead(n1.getId(), userId);
        assertTrue(markedOne);

        unread = notificationService.getUnreadNotifications(userId);
        assertEquals(1, unread.size());
        assertEquals("Hold Ready", unread.get(0).getTitle());

        boolean markedAll = notificationService.markAllAsRead(userId);
        assertTrue(markedAll);

        assertTrue(notificationService.getUnreadNotifications(userId).isEmpty());
        assertEquals(2, notificationService.getUserNotifications(userId).size());
    }
}

