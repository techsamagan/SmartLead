package com.samagan.smartlead.notification;

import com.samagan.smartlead.config.RabbitConfig;
import com.samagan.smartlead.lead.Lead;
import com.samagan.smartlead.lead.LeadQualifiedEvent;
import com.samagan.smartlead.lead.LeadRepository;
import com.samagan.smartlead.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class LeadNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(LeadNotificationListener.class);

    private final LeadRepository leadRepository;
    private final JavaMailSender mailSender;
    private final String from;
    private final String dashboardUrl;

    public LeadNotificationListener(LeadRepository leadRepository,
                                    JavaMailSender mailSender,
                                    @Value("${smartlead.mail.from}") String from,
                                    @Value("${smartlead.dashboard-url}") String dashboardUrl) {
        this.leadRepository = leadRepository;
        this.mailSender = mailSender;
        this.from = from;
        this.dashboardUrl = dashboardUrl;
    }

    // A failed send throws, so the listener retries it and then dead-letters the request
    @RabbitListener(queues = RabbitConfig.NOTIFICATION_QUEUE)
    public void onLeadQualified(LeadQualifiedEvent event) {
        Lead lead = leadRepository.findWithOwnerById(event.leadId()).orElse(null);
        // The form (and with it the lead) may have been deleted in the meantime
        if (lead == null) {
            log.info("Skipping notification, lead not found: {}", event.leadId());
            return;
        }
        Message message = lead.getMessage();

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(from);
        email.setTo(message.getForm().getUser().getEmail());
        // Replying to the notification writes straight to the potential customer
        email.setReplyTo(message.getSenderEmail());
        email.setSubject(singleLine("New lead: " + lead.getTitle()));
        email.setText("""
                You have a new potential customer from your form "%s".

                %s
                Type: %s
                Urgency: %s

                %s

                From: %s <%s>

                Their message:
                %s

                Reply to this email to answer them directly, or see all your leads at %s/leads
                """.formatted(
                message.getForm().getName(),
                lead.getTitle(),
                lead.getType(),
                lead.getUrgency(),
                lead.getSummary(),
                message.getSenderName(),
                message.getSenderEmail(),
                message.getContent(),
                dashboardUrl));

        mailSender.send(email);
    }

    // The title comes from the model; a line break in a subject would let it add mail headers
    private static String singleLine(String value) {
        return value.replaceAll("[\\r\\n]+", " ");
    }
}
