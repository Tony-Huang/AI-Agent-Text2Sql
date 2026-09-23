package com.hdp.ai_app_demo1.service;

import com.hdp.ai_app_demo1.tools.SqlTools;
import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.ollama.OllamaChatModel;
//import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Text2SqlService {

    private final Text2SqlAgent agent;

    private ChatModel chatModel;

    public Text2SqlService(ChatModel chatModel, SqlTools sqlTools) {
        this.chatModel = chatModel;
        this.agent = Text2SqlAgent.create(chatModel, sqlTools);
    }

    public String ask(String question) {
        return agent.chat(question);
    }
}
