package edu.eia.racing.dto;

import edu.eia.racing.model.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String username,
        String action,
        String entityType,
        Long entityId,
        LocalDateTime timestamp,
        String description,
        String oldValues,
        String newValues) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUser() == null ? null : auditLog.getUser().getId(),
                auditLog.getUser() == null ? null : auditLog.getUser().getUsername(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getTimestamp(),
                auditLog.getDescription(),
                auditLog.getOldValues(),
                auditLog.getNewValues());
    }
}
