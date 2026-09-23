package com.hdp.ai_app_demo1.controller;

import com.hdp.ai_app_demo1.service.Text2SqlService;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatModel chatModel;

    private final Text2SqlService text2SqlService;

    public ChatController(ChatModel chatModel, Text2SqlService text2SqlService) {
        this.chatModel = chatModel;
        this.text2SqlService = text2SqlService;
    }


    @GetMapping("/chat")
    public String model(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        System.out.println("chatModel = "+ chatModel + " , userInput=" + message);
        return text2SqlService.ask(message);
    }
}