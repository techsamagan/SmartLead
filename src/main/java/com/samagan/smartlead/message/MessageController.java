package com.samagan.smartlead.message;

import com.samagan.smartlead.lead.LeadQualificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final LeadQualificationService qualificationService;

    public MessageController(MessageRepository messageRepository,
                             LeadQualificationService qualificationService) {
        this.messageRepository = messageRepository;
        this.qualificationService = qualificationService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> submitMessage(@Valid @RequestBody CreateMessageRequest request) {
        Message message = new Message(
                request.senderName(),
                request.senderEmail(),
                request.content()
        );

        Message savedMessage = messageRepository.save(message);

        // Run qualification asynchronously
        qualificationService.qualifyAsync(savedMessage.getId());

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