package com.libraryos.repository;

import com.libraryos.model.interaction.Notification;

import java.util.List;

public interface NotificationRepository {
    List<Notification> findByUserId(Long userId);
    List<Notification> findUnreadByUserId(Long userId);
    Notification save(Notification notification);
    boolean markAsRead(Long id, Long userId);
    boolean markAllAsRead(Long userId);
}

