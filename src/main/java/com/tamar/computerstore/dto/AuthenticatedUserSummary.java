package com.tamar.computerstore.dto;

import com.tamar.computerstore.entity.UserRole;

/**
 * Identity fields that are safe to return to the caller. The password hash is deliberately absent.
 */
public record AuthenticatedUserSummary(
        Long id,
        String email,
        UserRole role,
        String firstName,
        String lastName
) {
}
