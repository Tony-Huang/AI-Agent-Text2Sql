package com.hdp.ai_app_demo1.tools;

import com.hdp.ai_app_demo1.service.AgentAuditLog;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SqlTools {

    private final JdbcTemplate jdbcTemplate;
    private final AgentAuditLog auditLog;
    private String currentSessionId;

    public SqlTools(JdbcTemplate jdbcTemplate, AgentAuditLog auditLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLog = auditLog;
    }

    public void setSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    @Tool("Execute read-only T-SQL SELECT query only. Never run DDL/DML statements. Table name with space must use bracket like [Order Details]. Return result or error message.")
    public String executeReadOnlySql(String sql) {
        auditLog.log(currentSessionId, "TOOL_INPUT", "executeReadOnlySql:" + sql);
        String lower = sql.toLowerCase().trim();
        var forbidden = java.util.List.of("drop", "alter", "create", "insert", "update", "delete", "merge", "truncate");
        if (!lower.startsWith("select")) {
            String rejectMsg = "Rejected: Only SELECT is allowed.";
            auditLog.log(currentSessionId, "TOOL_OUTPUT", rejectMsg);
            return rejectMsg;
        }
        for(String keyword : forbidden){
            if(lower.contains(keyword)){
                String rejectMsg = "Rejected: forbidden keyword found: " + keyword;
                auditLog.log(currentSessionId, "TOOL_OUTPUT", rejectMsg);
                return rejectMsg;
            }
        }
        try {
            String res = jdbcTemplate.queryForList(sql).toString();
            auditLog.log(currentSessionId, "TOOL_OUTPUT", res);
            return res;
        } catch (Exception e) {
            String errMsg = "SQL execution error: " + e.getMessage();
            auditLog.log(currentSessionId, "TOOL_OUTPUT", errMsg);
            return errMsg;
        }
    }
}
