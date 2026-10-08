package com.samagan.smartlead.lead;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        UUID messageId,
        String title,
        String type,
        String urgency,
        String summary,
        Instant createdAt
) {
    public static LeadResponse fromEntity(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getMessage().getId(),
                lead.getTitle(),
                lead.getType(),
                lead.getUrgency(),
                lead.getSummary(),
                lead.getCreatedAt()
        );
    }
}