package com.tamar.computerstore.dto;

import com.tamar.computerstore.entity.UserRole;

/**
 * Profile fields are null for an ADMIN account, which has no customer profile.
 */
public record CurrentUserResponse(
        Long id,
        String email,
        UserRole role,
        boolean enabled,
        String firstName,
        String lastName,
        String phone,
        String address
) {
}
