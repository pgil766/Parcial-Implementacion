package edu.eia.racing.dto;

import edu.eia.racing.model.enums.RoleName;

/**
 * Issued after a successful register, login or refresh.
 * Never carries the password hash.
 *
 * @param expiresIn access-token lifetime in seconds
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        RoleName role) {
}
