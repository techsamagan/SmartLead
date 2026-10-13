package com.samagan.smartlead.lead;

import com.samagan.smartlead.auth.TokenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    private final LeadRepository leadRepository;

    public LeadController(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @GetMapping
    public Page<LeadResponse> listLeads(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String urgency,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return leadRepository.findForUser(TokenService.userId(jwt), blankToNull(type), blankToNull(urgency), pageable)
                .map(LeadResponse::fromEntity);
    }

    // Another user's lead is reported as missing rather than forbidden, so ids cannot be probed
    @GetMapping("/{id}")
    public LeadResponse getLead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return leadRepository.findByIdAndMessageFormUserId(id, TokenService.userId(jwt))
                .map(LeadResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
