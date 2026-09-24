package com.hdp.ai_app_demo1.service;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagSchemaRetrieverService {

    private final SchemaIngestionService ingestionService;
    public final ChatModel chatModel;
    private final Text2SqlRagAgent ragAgent;

    public RagSchemaRetrieverService(SchemaIngestionService ingestionService, ChatModel chatModel) {
        this.ingestionService = ingestionService;
        // Native Qwen chat model
        this.chatModel = chatModel;

        // RAG retriever with Qwen embedding model
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(ingestionService.embeddingStore)
                .embeddingModel(ingestionService.embeddingModel)
                .maxResults(3)
                .minScore(0.3)   // text-embedding-v3 score scale differs from all-minilm; tune this
                .build();

        RetrievalAugmentor augment = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        this.ragAgent = AiServices.builder(Text2SqlRagAgent.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augment)
                .build();
    }

    public Text2SqlRagAgent getRagAgent() { return ragAgent; }

    public interface Text2SqlRagAgent {
        dev.langchain4j.service.Result<String> chat(@dev.langchain4j.service.UserMessage String userQuestion);
    }
}
