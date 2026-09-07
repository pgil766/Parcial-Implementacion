package edu.eia.racing.dto;

import edu.eia.racing.model.enums.RoleName;
import java.time.LocalDateTime;

/**
 * Read-only view of the authenticated account. Deliberately excludes the
 * password hash so it can never leak through the API.
 */
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        RoleName role,
        boolean enabled,
        LocalDateTime createdAt) {
}
