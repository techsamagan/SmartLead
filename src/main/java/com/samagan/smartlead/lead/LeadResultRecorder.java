package com.samagan.smartlead.lead;

import com.samagan.smartlead.message.Message;
import com.samagan.smartlead.message.MessageRepository;
import com.samagan.smartlead.message.MessageStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
public class LeadResultRecorder {

    // Must match the CHECK constraints in V1__init_schema.sql
    private static final Set<String> LEAD_TYPES =
            Set.of("demo-request", "pricing-inquiry", "partnership", "support", "other");
    private static final Set<String> URGENCIES = Set.of("low", "medium", "high");
    private static final int MAX_TITLE_LENGTH = 255;

    private final MessageRepository messageRepository;
    private final LeadRepository leadRepository;

    public LeadResultRecorder(MessageRepository messageRepository, LeadRepository leadRepository) {
        this.messageRepository = messageRepository;
        this.leadRepository = leadRepository;
    }

    @Transactional
    public void record(UUID messageId, LeadClassificationResult result) {
        Message message = messageRepository.findById(messageId).orElse(null);
        // A redelivered request may arrive after the message was already processed
        if (message == null || message.getStatus() != MessageStatus.PENDING) {
            return;
        }

        if (result.isLead()) {
            Lead lead = Lead.builder()
                    .message(message)
                    .title(truncate(result.title() != null ? result.title() : "Inbound Lead"))
                    .type(normalize(result.type(), LEAD_TYPES, "other"))
                    .urgency(normalize(result.urgency(), URGENCIES, "medium"))
                    .summary(result.summary() != null ? result.summary() : message.getContent())
                    .build();

            leadRepository.save(lead);
            message.setStatus(MessageStatus.QUALIFIED);
        } else {
            message.setStatus(MessageStatus.NOT_A_LEAD);
        }
        message.setProcessedAt(Instant.now());
    }

    @Transactional
    public void markFailed(UUID messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null || message.getStatus() != MessageStatus.PENDING) {
            return;
        }
        message.setStatus(MessageStatus.FAILED);
        message.setProcessedAt(Instant.now());
    }

    private static String normalize(String value, Set<String> allowed, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase().replaceAll("[\\s_]+", "-");
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private static String truncate(String title) {
        return title.length() <= MAX_TITLE_LENGTH ? title : title.substring(0, MAX_TITLE_LENGTH);
    }
}
