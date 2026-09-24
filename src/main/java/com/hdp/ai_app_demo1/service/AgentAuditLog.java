package com.hdp.ai_app_demo1.service;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AgentAuditLog {

    private final List<AuditRecord> records = new ArrayList<>();

    public void log(String sessionId, String type, String content) {
        AuditRecord r = AuditRecord.builder()
                .sessionId(sessionId)
                .type(type)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
        records.add(r);
    }

    public List<AuditRecord> getBySessionId(String sessionId) {
        return records.stream().filter(r -> r.sessionId.equals(sessionId)).toList();
    }

    @Data
    @Builder
    public static class AuditRecord {
        String sessionId;
        String type;
        String content;
        long timestamp;
    }
}
