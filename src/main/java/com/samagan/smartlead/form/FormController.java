package com.samagan.smartlead.form;

import com.samagan.smartlead.auth.TokenService;
import com.samagan.smartlead.auth.UserAccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms")
public class FormController {

    private final FormRepository formRepository;
    private final UserAccountRepository userRepository;

    public FormController(FormRepository formRepository, UserAccountRepository userRepository) {
        this.formRepository = formRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<FormResponse> listForms(@AuthenticationPrincipal Jwt jwt) {
        return formRepository.findByUserIdOrderByCreatedAtAsc(TokenService.userId(jwt))
                .stream()
                .map(FormResponse::fromEntity)
                .toList();
    }

    @PostMapping
    public ResponseEntity<FormResponse> createForm(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CreateFormRequest request) {
        Form form = new Form(userRepository.getReferenceById(TokenService.userId(jwt)), request.name().trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FormResponse.fromEntity(formRepository.save(form)));
    }

    // Also removes the form's messages and leads (ON DELETE CASCADE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Form form = formRepository.findByIdAndUserId(id, TokenService.userId(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
        formRepository.delete(form);
        return ResponseEntity.noContent().build();
    }
}
