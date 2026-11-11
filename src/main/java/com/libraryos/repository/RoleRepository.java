package com.libraryos.repository;

import com.libraryos.model.user.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository {
    List<Role> findAll();
    Optional<Role> findById(int id);
    Optional<Role> findByCode(String code);
    Set<Role> findRolesByUserId(Long userId);
}
