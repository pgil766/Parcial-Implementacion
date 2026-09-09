package edu.eia.racing.service;

import edu.eia.racing.dto.AuditLogResponse;
import edu.eia.racing.exception.ResourceNotFoundException;
import edu.eia.racing.model.AuditLog;
import edu.eia.racing.model.User;
import edu.eia.racing.repository.AuditLogRepository;
import edu.eia.racing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(AuditLogResponse::from);
    }

    @Transactional
    public void recordCurrentUser(String action, String entityType, Long entityId, String description) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        record(user, action, entityType, entityId, description);
    }

    @Transactional
    public void record(User user, String action, String entityType, Long entityId, String description) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build());
    }
}
