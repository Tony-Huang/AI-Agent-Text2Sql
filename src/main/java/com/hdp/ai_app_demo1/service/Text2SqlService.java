package com.hdp.ai_app_demo1.service;

import com.hdp.ai_app_demo1.tools.SqlTools;
import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.ollama.OllamaChatModel;
//import dev.langchain4j.model.chat.ChatLanguageModel;
//import org.springframework.beans.factory.annotation.Autowired;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class Text2SqlService {

    private final RagSchemaRetrieverService ragSchemaRetrieverService;
    private final SqlTools sqlTools;
    private final AgentAuditLog auditLog;

    public Text2SqlService(RagSchemaRetrieverService ragSchemaRetrieverService,
                                SqlTools sqlTools,
                                AgentAuditLog auditLog) {
        this.ragSchemaRetrieverService = ragSchemaRetrieverService;
        this.sqlTools = sqlTools;
        this.auditLog = auditLog;
    }

    public AgentResult ask(String userQuestion) {
        String sessionId = UUID.randomUUID().toString();
        auditLog.log(sessionId, "USER_QUESTION", userQuestion);
        sqlTools.setSessionId(sessionId);

        // Step 1: RAG retrieve relevant schema chunks
        var ragResult = ragSchemaRetrieverService.getRagAgent().chat(userQuestion);
        String schemaContext = ragResult.content();
        auditLog.log(sessionId, "RAG_SCHEMA_CONTEXT", schemaContext);

        // Step2: Build agent with SQL execution tool
        Text2SqlAgent agent = AiServices.builder(Text2SqlAgent.class)
                .chatModel(ragSchemaRetrieverService.chatModel)
                .tools(sqlTools)
                .build();

        String prompt = """
                You are a T-SQL Text-to-SQL agent working on SQL Server Northwind database.
                Rules:
                1. Use ONLY the retrieved schema context below. Never invent tables or columns.
                2. Table name with space must use bracket syntax, e.g. [Order Details].
                3. Use foreign key relationships to write JOIN when needed.
                4. Only generate SELECT T-SQL. No DDL/DML.
                5. You can call executeReadOnlySql tool to run SQL. Max total tool calls =3.
                6. After you get query result, summarize answer in natural language.

                === Retrieved Northwind Schema Context ===
                """ + schemaContext + """
                === User Question ===
                """ + userQuestion;

        String finalAnswer = agent.chat(prompt);
        auditLog.log(sessionId, "FINAL_ANSWER", finalAnswer);

        return AgentResult.builder()
                .sessionId(sessionId)
                .answer(finalAnswer)
                .auditRecords(auditLog.getBySessionId(sessionId))
                .build();
    }

    public interface Text2SqlAgent {
        @SystemMessage("You are T-SQL expert for Northwind. Use provided schema. Use bracket for table name with space.")
        String chat(@UserMessage String prompt);
    }

    @Data
    @Builder
    public static class AgentResult {
        String sessionId;
        String answer;
        java.util.List<AgentAuditLog.AuditRecord> auditRecords;
    }
}
