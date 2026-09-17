package com.tamar.computerstore.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthenticatedUserSummary user
) {

    public static AuthResponse bearer(String accessToken, long expiresIn, AuthenticatedUserSummary user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}
