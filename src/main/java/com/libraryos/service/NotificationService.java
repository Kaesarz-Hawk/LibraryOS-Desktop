package com.libraryos.service;

import com.libraryos.model.interaction.Notification;
import com.libraryos.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public Notification sendNotification(Long userId, String title, String message, String type) {
        Notification notif = new Notification(userId, title, message, type);
        Notification saved = notificationRepository.save(notif);
        log.info("Sent notification to user {}: [{}] {}", userId, type, title);
        return saved;
    }

    public boolean markAsRead(Long id, Long userId) {
        return notificationRepository.markAsRead(id, userId);
    }

    public boolean markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }
}

