package com.samagan.smartlead.form;

import com.samagan.smartlead.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "forms")
public class Form {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, length = 100)
    private String name;

    // Public identifier used in the submission URL; unguessable, unlike the id it is safe to publish
    @Column(name = "form_key", nullable = false, unique = true, length = 32)
    private String formKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Form() {}

    public Form(UserAccount user, String name) {
        this.user = user;
        this.name = name;
        this.formKey = newFormKey();
    }

    private static String newFormKey() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public UUID getId() { return id; }

    public UserAccount getUser() { return user; }

    public String getName() { return name; }

    public String getFormKey() { return formKey; }

    public Instant getCreatedAt() { return createdAt; }
}
