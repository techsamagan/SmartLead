package com.samagan.smartlead.notification;

import com.samagan.smartlead.config.RabbitConfig;
import com.samagan.smartlead.lead.LeadQualifiedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LeadNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeadNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public LeadNotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Queued only after the lead is committed, so the consumer can always load it
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeadQualified(LeadQualifiedEvent event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.NOTIFICATION_ROUTING_KEY, event);
        } catch (AmqpException e) {
            // The lead is saved and visible in the dashboard; only the email is lost
            log.error("Could not queue notification for leadId={}", event.leadId(), e);
        }
    }
}
