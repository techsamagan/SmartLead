package com.samagan.smartlead.lead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeadClassificationResult(
        boolean isLead,
        String title,
        String type,    // demo-request, pricing-inquiry, partnership, support, other
        String urgency, // low, medium, high
        String summary
) {
}
