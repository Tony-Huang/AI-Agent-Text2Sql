package com.hdp.ai_app_demo1.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SqlTools {

    private final JdbcTemplate jdbcTemplate;

    public SqlTools(JdbcTemplate JdbcTemplate) {
        this.jdbcTemplate = JdbcTemplate;
    }

    /**
     * Get table DDL schema
     */
    @Tool("Get DDL schema of a table. Call this when you need table or column information.")
    public String getTableSchema(String tableName) {
        String sql = """
            SELECT COLUMN_NAME, DATA_TYPE,
                   CHARACTER_MAXIMUM_LENGTH AS MAX_LEN,
                   IS_NULLABLE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = ?
            ORDER BY ORDINAL_POSITION
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tableName);
        if (rows.isEmpty()) {
            return "Table not found: " + tableName;
        }
        StringBuilder sb = new StringBuilder("Table: ").append(tableName).append("\n");
        for (Map<String, Object> r : rows) {
            sb.append(" - ").append(r.get("COLUMN_NAME"))
                    .append(" ").append(r.get("DATA_TYPE"));
            Object len = r.get("MAX_LEN");
            if (len != null) sb.append("(").append(len).append(")");
            sb.append(r.get("IS_NULLABLE").equals("YES") ? " NULL" : " NOT NULL")
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * Execute read-only SELECT SQL only
     */
    @Tool("Execute read-only SELECT SQL query, return query result. Do NOT run DROP/ALTER/INSERT/UPDATE/DELETE.")
    public String executeReadOnlySql(String sql) {
        String lower = sql.toLowerCase().trim();
        if (!lower.startsWith("select")) {
            return "Rejected: only SELECT statements are allowed.";
        }
        for (String forbidden : new String[]{
                "drop", "alter", "create", "insert", "update", "delete", "merge", "truncate"}) {
            if (lower.contains(forbidden)) {
                return "Rejected: statement contains forbidden keyword: " + forbidden;
            }
        }
        try {
            return jdbcTemplate.queryForList(sql).toString();
        } catch (Exception e) {
            // Return the error to the agent so it can fix the SQL and retry.
            return "SQL execution failed: " + e.getMessage();
        }
    }
}
