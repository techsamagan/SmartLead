package com.samagan.smartlead.lead;

import com.samagan.smartlead.message.Message;
import com.samagan.smartlead.message.MessageRepository;
import com.samagan.smartlead.message.MessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class LeadQualificationService {

    private static final Logger log = LoggerFactory.getLogger(LeadQualificationService.class);

    private final LeadClassifier leadClassifier;
    private final MessageRepository messageRepository;
    private final LeadResultRecorder resultRecorder;

    public LeadQualificationService(LeadClassifier leadClassifier,
                                    MessageRepository messageRepository,
                                    LeadResultRecorder resultRecorder) {
        this.leadClassifier = leadClassifier;
        this.messageRepository = messageRepository;
        this.resultRecorder = resultRecorder;
    }

    // Not transactional: the model call must not hold a database connection open.
    // Any exception that escapes is treated as transient and retried by the listener.
    public void qualify(UUID messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("Skipping qualification, message not found: {}", messageId);
            return;
        }
        if (message.getStatus() != MessageStatus.PENDING) {
            log.info("Skipping qualification, messageId={} is already {}", messageId, message.getStatus());
            return;
        }

        LeadClassificationResult result;
        try {
            result = leadClassifier.classify(message.getContent());
        } catch (NonTransientAiException e) {
            // Bad credentials, unknown model, no credits: retrying will not help
            log.error("Model rejected the request for messageId={}", messageId, e);
            resultRecorder.markFailed(messageId);
            return;
        }

        if (result == null) {
            throw new IllegalStateException("Model returned an empty response for messageId=" + messageId);
        }
        resultRecorder.record(messageId, result);
    }
}
