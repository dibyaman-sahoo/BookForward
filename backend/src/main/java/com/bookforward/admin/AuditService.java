package com.bookforward.admin;

import com.bookforward.entity.AuditLog;
import com.bookforward.repository.AuditLogRepository;
import com.bookforward.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records sensitive/admin actions. Never pass secrets or tokens in metadata. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository logs;
    private final UserRepository users;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(UUID actorId, String action, String resourceType, Object resourceId, String metadata) {
        AuditLog l = new AuditLog();
        if (actorId != null) l.setActor(users.getReferenceById(actorId));
        l.setAction(action);
        l.setResourceType(resourceType);
        l.setResourceId(resourceId == null ? null : resourceId.toString());
        l.setMetadata(metadata != null && metadata.length() > 2000 ? metadata.substring(0, 2000) : metadata);
        logs.save(l);
    }
}
