package com.petlink.infrastructure.audit.service;

import com.petlink.infrastructure.audit.entity.OperationLog;
import com.petlink.infrastructure.audit.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OperationLogService {
    private static final Map<String, Set<String>> ALLOWED_OPERATIONS = Map.of(
            "SYS_USER", Set.of("ENABLE", "DISABLE", "PROMOTE_RESCUER"),
            "RESCUE_CLUE", Set.of("CREATE", "WITHDRAW", "AUDIT_APPROVE", "AUDIT_REJECT",
                    "ACCEPT_RESCUE", "REOPEN", "CLOSE_AFTER_RESCUE", "CLOSE_AFTER_FAILURE"),
            "RESCUE_TASK", Set.of("START_RESCUE", "COMPLETE_RESCUE", "RESCUE_FAILED", "CANCEL_RESCUE"),
            "ANIMAL", Set.of("TO_OBSERVING", "OPEN_ADOPTION", "SUSPEND_ADOPTION", "RESUME_ADOPTION", "ADOPT"),
            "ADOPTION_APPLICATION", Set.of("CREATE", "WITHDRAW", "AUDIT_APPROVE", "AUDIT_REJECT", "AUTO_INVALIDATE"),
            "ANNOUNCEMENT", Set.of("CREATE", "PUBLISH", "WITHDRAW")
    );

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void append(String businessType, Long businessId, String operationType,
                       String beforeStatus, String afterStatus, Long operatorId, String reason) {
        Set<String> allowed = ALLOWED_OPERATIONS.get(businessType);
        if (allowed == null || !allowed.contains(operationType)) {
            throw new IllegalArgumentException("Unsupported operation log type: " + businessType + "/" + operationType);
        }
        OperationLog log = new OperationLog();
        log.setBusinessType(businessType);
        log.setBusinessId(businessId);
        log.setOperationType(operationType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setOperatorId(operatorId);
        log.setReason(reason);
        if (mapper.insert(log) != 1) {
            throw new IllegalStateException("operation_log insert affected rows != 1");
        }
    }
}
