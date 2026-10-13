package com.samagan.smartlead.lead;

import com.samagan.smartlead.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LeadQualificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    public LeadQualificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(UUID messageId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.QUALIFICATION_ROUTING_KEY,
                new LeadQualificationRequest(messageId)
        );
    }
}
