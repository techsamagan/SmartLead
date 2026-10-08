package com.samagan.smartlead.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samagan.smartlead.client.HuggingFaceClient;
import com.samagan.smartlead.client.HuggingFaceRequest;
import com.samagan.smartlead.client.HuggingFaceResponse;
import com.samagan.smartlead.message.Message;
import com.samagan.smartlead.message.MessageRepository;
import com.samagan.smartlead.message.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@Service
public class LeadQualificationService {

    private static final Logger log = LoggerFactory.getLogger(LeadQualificationService.class);

    private final HuggingFaceClient hfClient;
    private final MessageRepository messageRepository;
    private final LeadRepository leadRepository;
    private ObjectMapper objectMapper;

    @Value("${ai.huggingface.model:meta-llama/Llama-3.3-70B-Instruct:groq}")
    private String model;

    public LeadQualificationService(HuggingFaceClient hfClient,
                                    MessageRepository messageRepository,
                                    LeadRepository leadRepository,
                                    ObjectMapper objectMapper) {
        this.hfClient = hfClient;
        this.messageRepository = messageRepository;
        this.leadRepository = leadRepository;
        this.objectMapper = objectMapper;
    }

    @Async("leadTaskExecutor")
    @Transactional
    public void qualifyAsync(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        try {
            HuggingFaceRequest request = HuggingFaceRequest.create(
                    model,
                    LeadPrompt.SYSTEM_PROMPT,
                    message.getContent()
            );

            HuggingFaceResponse response = hfClient.complete(request);
            String rawJson = sanitizeJson(response.content());
            LeadClassificationResult result = objectMapper.readValue(rawJson, LeadClassificationResult.class);

            if (result.isLead()) {
                Lead lead = Lead.builder()
                        .message(message)
                        .title(result.title() != null ? result.title() : "Inbound Lead")
                        .type(result.type() != null ? result.type().toLowerCase() : "other")
                        .urgency(result.urgency() != null ? result.urgency().toLowerCase() : "medium")
                        .summary(result.summary() != null ? result.summary() : message.getContent())
                        .build();

                leadRepository.save(lead);
                message.setStatus(MessageStatus.QUALIFIED);
            } else {
                message.setStatus(MessageStatus.NOT_A_LEAD);
            }
        } catch (Exception e) {
            log.error("Failed to process message qualification for messageId={}", messageId, e);
            message.setStatus(MessageStatus.FAILED);
        } finally {
            message.setProcessedAt(Instant.now());
            messageRepository.save(message);
        }
    }

    private String sanitizeJson(String content) {
        String clean = content.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }
}
