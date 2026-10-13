package com.samagan.smartlead.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitConfig {

    public static final String EXCHANGE = "smartlead.leads";
    public static final String QUALIFICATION_QUEUE = "lead.qualification";
    public static final String QUALIFICATION_ROUTING_KEY = "lead.qualify";

    public static final String DEAD_LETTER_EXCHANGE = "smartlead.leads.dlx";
    public static final String DEAD_LETTER_QUEUE = "lead.qualification.dlq";

    public static final String NOTIFICATION_QUEUE = "lead.notification";
    public static final String NOTIFICATION_ROUTING_KEY = "lead.notify";
    public static final String NOTIFICATION_DEAD_LETTER_QUEUE = "lead.notification.dlq";

    @Bean
    public DirectExchange leadExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public DirectExchange leadDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    // Messages rejected after all retries are routed to the dead-letter queue
    @Bean
    public Queue qualificationQueue() {
        return QueueBuilder.durable(QUALIFICATION_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue qualificationDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding qualificationBinding() {
        return BindingBuilder.bind(qualificationQueue()).to(leadExchange()).with(QUALIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding qualificationDeadLetterBinding() {
        return BindingBuilder.bind(qualificationDeadLetterQueue()).to(leadDeadLetterExchange()).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(NOTIFICATION_DEAD_LETTER_QUEUE)
                .build();
    }

    // Emails that could not be sent after all retries are parked here for inspection
    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(leadExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationDeadLetterBinding() {
        return BindingBuilder.bind(notificationDeadLetterQueue()).to(leadDeadLetterExchange())
                .with(NOTIFICATION_DEAD_LETTER_QUEUE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
