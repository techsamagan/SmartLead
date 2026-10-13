package com.samagan.smartlead.message;

import com.samagan.smartlead.form.Form;
import com.samagan.smartlead.form.FormRepository;
import com.samagan.smartlead.lead.LeadQualificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
public class MessageSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(MessageSubmissionService.class);

    private final FormRepository formRepository;
    private final MessageRepository messageRepository;
    private final SubmissionRateLimiter rateLimiter;
    private final LeadQualificationPublisher qualificationPublisher;
    private final long monthlyQuota;

    public MessageSubmissionService(FormRepository formRepository,
                                    MessageRepository messageRepository,
                                    SubmissionRateLimiter rateLimiter,
                                    LeadQualificationPublisher qualificationPublisher,
                                    @Value("${smartlead.public-form.monthly-quota:500}") long monthlyQuota) {
        this.formRepository = formRepository;
        this.messageRepository = messageRepository;
        this.rateLimiter = rateLimiter;
        this.qualificationPublisher = qualificationPublisher;
        this.monthlyQuota = monthlyQuota;
    }

    public Message submit(String formKey, CreateMessageRequest request, String clientAddress) {
        Form form = formRepository.findByFormKey(formKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));

        if (!rateLimiter.tryAcquire(formKey + "|" + clientAddress)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many submissions, try again in a minute");
        }
        // Every submission costs a model call, so each account is capped per calendar month
        Instant monthStart = YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (messageRepository.countByFormUserIdAndCreatedAtAfter(form.getUser().getId(), monthStart) >= monthlyQuota) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "This form has reached its monthly limit");
        }

        Message savedMessage = messageRepository.save(
                new Message(form, request.senderName(), request.senderEmail(), request.content()));

        // The message is already committed; qualification happens asynchronously via RabbitMQ
        try {
            qualificationPublisher.publish(savedMessage.getId());
        } catch (AmqpException e) {
            // PendingMessageSweeper queues it again once the broker is back
            log.warn("Could not queue messageId={} for qualification", savedMessage.getId(), e);
        }
        return savedMessage;
    }
}
