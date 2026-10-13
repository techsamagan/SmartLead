package com.samagan.smartlead;

import com.fasterxml.jackson.databind.JsonNode;
import com.samagan.smartlead.auth.UserAccountRepository;
import com.samagan.smartlead.form.FormRepository;
import com.samagan.smartlead.lead.LeadRepository;
import com.samagan.smartlead.message.Message;
import com.samagan.smartlead.message.MessageRepository;
import com.samagan.smartlead.message.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.rabbitmq.listener.simple.retry.initial-interval=100ms"
        })
abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "correct-horse-battery";

    protected static final String LEAD_REPLY = """
            {"isLead": true, "title": "Demo request", "type": "demo-request",
             "urgency": "medium", "summary": "Wants a demo."}
            """;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected MessageRepository messageRepository;

    @Autowired
    protected LeadRepository leadRepository;

    @Autowired
    protected FormRepository formRepository;

    @Autowired
    protected UserAccountRepository userRepository;

    // The real model is never called; each test scripts its reply
    @MockitoBean
    protected ChatModel chatModel;

    // No mail server in tests; sent emails are inspected on the mock
    @MockitoBean
    protected JavaMailSender mailSender;

    @BeforeEach
    void cleanDatabase() {
        leadRepository.deleteAll();
        messageRepository.deleteAll();
        formRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected void modelReplies(String json) {
        when(chatModel.call(any(Prompt.class))).thenReturn(modelReply(json));
    }

    protected static ChatResponse modelReply(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    protected String register(String email) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/register", Map.of("email", email, "password", PASSWORD), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("token").asText();
    }

    protected ResponseEntity<JsonNode> get(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
    }

    protected static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    // Every account is created with one form
    protected String firstFormKey(String token) {
        return get("/api/v1/forms", token).getBody().get(0).get("formKey").asText();
    }

    protected UUID submit(String formKey, String content) {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/forms/" + formKey + "/messages",
                Map.of("senderName", "Test Sender", "senderEmail", "sender@example.com", "content", content),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return UUID.fromString(response.getBody().get("id").asText());
    }

    protected Message awaitProcessed(UUID messageId) {
        return await().atMost(Duration.ofSeconds(15)).until(
                () -> messageRepository.findById(messageId).orElseThrow(),
                message -> message.getStatus() != MessageStatus.PENDING);
    }
}
