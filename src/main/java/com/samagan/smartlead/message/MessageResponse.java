package com.samagan.smartlead.message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
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
                message.getSenderName(),
                message.getSenderEmail(),
                message.getContent(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getProcessedAt()
        );
    }
}