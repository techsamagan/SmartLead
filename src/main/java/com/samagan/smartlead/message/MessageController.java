package com.samagan.smartlead.message;

import com.samagan.smartlead.lead.LeadQualificationPublisher;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageRepository messageRepository;
    private final LeadQualificationPublisher qualificationPublisher;

    public MessageController(MessageRepository messageRepository,
                             LeadQualificationPublisher qualificationPublisher) {
        this.messageRepository = messageRepository;
        this.qualificationPublisher = qualificationPublisher;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> submitMessage(@Valid @RequestBody CreateMessageRequest request) {
        Message message = new Message(
                request.senderName(),
                request.senderEmail(),
                request.content()
        );

        Message savedMessage = messageRepository.save(message);

        // The message is already committed; qualification happens asynchronously via RabbitMQ
        try {
            qualificationPublisher.publish(savedMessage.getId());
        } catch (AmqpException e) {
            // PendingMessageSweeper queues it again once the broker is back
            log.warn("Could not queue messageId={} for qualification", savedMessage.getId(), e);
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(MessageResponse.fromEntity(savedMessage));
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getAllMessages() {
        List<MessageResponse> messages = messageRepository.findAll()
                .stream()
                .map(MessageResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(messages);
    }
}