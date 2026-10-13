package com.samagan.smartlead.auth;

import com.samagan.smartlead.form.Form;
import com.samagan.smartlead.form.FormRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserAccountRepository userRepository;
    private final FormRepository formRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UserAccountRepository userRepository,
                          FormRepository formRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.userRepository = userRepository;
        this.formRepository = formRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        UserAccount user = userRepository.save(new UserAccount(email, passwordEncoder.encode(request.password())));
        // Every account starts with one form so there is a URL to use right away
        formRepository.save(new Form(user, "My website"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(tokenService.issue(user), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = userRepository.findByEmail(normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return ResponseEntity.ok(new AuthResponse(tokenService.issue(user), user.getEmail()));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
