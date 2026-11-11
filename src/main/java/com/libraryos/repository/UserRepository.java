package com.libraryos.repository;

import com.libraryos.model.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    List<User> findAll();
    User create(User user);
    void update(User user);
    void updateFailedAttempts(Long userId, int attempts, LocalDateTime lockedUntil);
    void resetFailedAttempts(Long userId);
    void updateLastLogin(Long userId, LocalDateTime loginTime);
    void assignRole(Long userId, int roleId);
    void removeRole(Long userId, int roleId);
}
