package com.samagan.smartlead.lead;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class LeadClassifier {

    private final ChatClient chatClient;

    public LeadClassifier(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(LeadPrompt.SYSTEM_PROMPT)
                .build();
    }

    public LeadClassificationResult classify(String messageContent) {
        return chatClient.prompt()
                .user(messageContent)
                .call()
                .entity(LeadClassificationResult.class);
    }
}
