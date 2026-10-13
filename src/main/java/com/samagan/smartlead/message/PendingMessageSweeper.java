package com.samagan.smartlead.message;

import com.samagan.smartlead.lead.LeadQualificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// Closes the gap between the database and the broker: a message that was saved
// but never published (or whose request was lost) is queued again.
@Component
public class PendingMessageSweeper {

    private static final Logger log = LoggerFactory.getLogger(PendingMessageSweeper.class);
    private static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private final MessageRepository messageRepository;
    private final LeadQualificationPublisher publisher;

    public PendingMessageSweeper(MessageRepository messageRepository, LeadQualificationPublisher publisher) {
        this.messageRepository = messageRepository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void requeueStaleMessages() {
        List<Message> stale = messageRepository.findByStatusAndCreatedAtBefore(
                MessageStatus.PENDING, Instant.now().minus(STALE_AFTER));
        if (stale.isEmpty()) {
            return;
        }

        log.warn("Re-queueing {} message(s) stuck in PENDING", stale.size());
        try {
            stale.forEach(message -> publisher.publish(message.getId()));
        } catch (AmqpException e) {
            log.error("Could not re-queue pending messages, will try again on the next run", e);
        }
    }
}
