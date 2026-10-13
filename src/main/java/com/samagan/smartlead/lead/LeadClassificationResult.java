package com.samagan.smartlead.lead;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeadClassificationResult(
        @JsonProperty("isLead") boolean isLead,
        String title,
        String type,    // demo-request, pricing-inquiry, partnership, support, other
        String urgency, // low, medium, high
        String summary
) {
}
