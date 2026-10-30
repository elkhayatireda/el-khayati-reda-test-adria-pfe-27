package org.example.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class EbankAIAgent {

    private final ChatClient chatClient;

    public EbankAIAgent(ChatClient.Builder chatClientBuilder,
                        @Autowired(required = false) ChatMemory chatMemory,
                        @Autowired(required = false) ToolCallbackProvider tools) {
        var builder = chatClientBuilder
            .defaultSystem("""
                Vous êtes un assistant qui se charge de répondre aux questions
                de l'utilisateur à propos des clients et des comptes bancaires, en fonction du 
                contexte fourni.
                Si aucun contexte n'est fourni, réponds avec JE NE SAIS PAS
                """);

        if (chatMemory != null) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        }

        if (tools != null) {
            builder.defaultToolCallbacks(tools);
        }

        this.chatClient = builder.build();
    }

    public String chat(Prompt prompt, String conversationId) {
        return chatClient.prompt(prompt)
            .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
            .call().content();
    }

    public Flux<String> chatStream(Prompt prompt, String conversationId) {
        return chatClient.prompt(prompt)
            .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
            .stream().content();
    }
}
