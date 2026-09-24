package com.hdp.ai_app_demo1.service;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SchemaIngestionService {
    private final JdbcTemplate jdbcTemplate;
    public final EmbeddingModel embeddingModel ;
    public final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    public SchemaIngestionService(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
    }

    /**
     * One-time sync job, read Northwind schema from SQL Server system tables
     * Extract tables, columns, PK, FK, build chunk text and embed into vector store
     */
    public void syncAllSchemaToVectorDB() {
        embeddingStore.removeAll();

        // Step1: Get all user tables in Northwind
        String tableQuery = """
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_TYPE='BASE TABLE'
                """;
        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(tableQuery);

        for (Map<String, Object> tableMap : tableRows) {
            String tableName = (String) tableMap.get("TABLE_NAME");

            // Step2: Get columns, data type, nullable
            String columnQuery = """
                    SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_NAME = ?
                    ORDER BY ORDINAL_POSITION
                    """;
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(columnQuery, tableName);

            // Step3: Get Primary Key columns
            String pkQuery = """
                    SELECT COLUMN_NAME
                    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                    WHERE TABLE_NAME = ? AND CONSTRAINT_NAME LIKE 'PK_%'
                    """;
            List<Map<String, Object>> pkRows = jdbcTemplate.queryForList(pkQuery, tableName);
            List<String> pkCols = pkRows.stream().map(r -> (String) r.get("COLUMN_NAME")).toList();

            // Step4: Get Foreign Keys (handle self-reference like Employees.ReportsTo)
            String fkQuery = """
                    SELECT
                        OBJECT_NAME(fk.parent_object_id) AS child_table,
                        col1.name AS child_column,
                        OBJECT_NAME(fk.referenced_object_id) AS parent_table,
                        col2.name AS parent_column
                    FROM sys.foreign_keys fk
                    INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
                    INNER JOIN sys.columns col1 ON fkc.parent_object_id = col1.object_id AND fkc.parent_column_id = col1.column_id
                    INNER JOIN sys.columns col2 ON fkc.referenced_object_id = col2.object_id AND fkc.referenced_column_id = col2.column_id
                    WHERE OBJECT_NAME(fk.parent_object_id)=? OR OBJECT_NAME(fk.referenced_object_id)=?
                    """;
            List<Map<String, Object>> fkRows = jdbcTemplate.queryForList(fkQuery, tableName, tableName);

            // Build chunk text for this table
            StringBuilder chunk = new StringBuilder();
            chunk.append("Table: ").append(tableName).append("\n");
            chunk.append("Primary Key: ").append(String.join(",", pkCols)).append("\n");
            chunk.append("Columns:\n");
            for (Map<String, Object> col : columns) {
                chunk.append(" - ")
                        .append(col.get("COLUMN_NAME"))
                        .append(" | DataType: ").append(col.get("DATA_TYPE"))
                        .append(" | Nullable: ").append(col.get("IS_NULLABLE"))
                        .append("\n");
            }
            chunk.append("Foreign Keys:\n");
            if (fkRows.isEmpty()) {
                chunk.append(" No foreign keys\n");
            } else {
                for (Map<String, Object> fk : fkRows) {
                    chunk.append(String.format(" %s.%s --> %s.%s\n",
                            fk.get("child_table"), fk.get("child_column"),
                            fk.get("parent_table"), fk.get("parent_column")));
                }
            }

            // Important note for LLM: table name with space must use [ ] bracket
            if (tableName.contains(" ")) {
                chunk.append("\nWARNING: this table name contains space, use bracket [" + tableName + "] in T-SQL.\n");
            }

            TextSegment segment = TextSegment.from(chunk.toString());
            embeddingStore.add(embeddingModel.embed(segment).content(), segment);
        }
    }
}
