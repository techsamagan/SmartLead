package com.samagan.smartlead.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID formId,
        String formName,
        String senderName,
        String senderEmail,
        String content,
        MessageStatus status,
        Instant createdAt,
        Instant processedAt
) {
    public static MessageResponse fromEntity(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getForm().getId(),
                message.getForm().getName(),
                message.getSenderName(),
                message.getSenderEmail(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getProcessedAt()
        );
    }
}
