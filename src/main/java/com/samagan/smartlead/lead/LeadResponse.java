package com.samagan.smartlead.lead;

import com.samagan.smartlead.message.Message;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        UUID messageId,
        UUID formId,
        String formName,
        String senderName,
        String senderEmail,
        String content,
        String title,
        String type,
        String urgency,
        String summary,
        Instant createdAt
) {
    public static LeadResponse fromEntity(Lead lead) {
        Message message = lead.getMessage();
        return new LeadResponse(
                lead.getId(),
                message.getId(),
                message.getForm().getId(),
                message.getForm().getName(),
                message.getSenderName(),
                message.getSenderEmail(),
                message.getContent(),
                lead.getTitle(),
                lead.getType(),
                lead.getUrgency(),
                lead.getSummary(),
                lead.getCreatedAt()
        );
    }
}
