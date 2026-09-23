package com.hdp.ai_app_demo1.service;

import com.hdp.ai_app_demo1.tools.SqlTools;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface Text2SqlAgent {

    @SystemMessage("""
            You are a Text-to-SQL agent for Microsoft SQL Server (T-SQL).
            Rules:
            1. First call getTableSchema to understand columns.
            2. Generate only SELECT read-only T-SQL.
            3. Execute SQL via executeReadOnlySql.
            4. If SQL fails, read the error, fix the SQL, and retry (max 3 times).
            5. Summarize the final result in natural language.
            """)
    String chat(@UserMessage String userQuestion);

    static Text2SqlAgent create(ChatModel model, SqlTools sqlTools) {
        return AiServices.builder(Text2SqlAgent.class)
                .chatModel(model)
                .tools(sqlTools)
                .build();
    }

}
