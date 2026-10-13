package com.samagan.smartlead;

import com.samagan.smartlead.lead.Lead;
import com.samagan.smartlead.lead.LeadRepository;
import com.samagan.smartlead.message.CreateMessageRequest;
import com.samagan.smartlead.message.Message;
import com.samagan.smartlead.message.MessageRepository;
import com.samagan.smartlead.message.MessageResponse;
import com.samagan.smartlead.message.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.rabbitmq.listener.simple.retry.initial-interval=100ms"
        })
class LeadQualificationIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private LeadRepository leadRepository;

    // The real model is never called; each test scripts its reply
    @MockitoBean
    private ChatModel chatModel;

    @BeforeEach
    void cleanDatabase() {
        leadRepository.deleteAll();
        messageRepository.deleteAll();
    }

    @Test
    void qualifiedMessageBecomesLead() {
        when(chatModel.call(any(Prompt.class))).thenReturn(modelReply("""
                {"isLead": true, "title": "Enterprise Demo - 50 engineers", "type": "Pricing Inquiry",
                 "urgency": "HIGH", "summary": "Team of 50 wants a demo and pricing."}
                """));

        UUID messageId = submit("We are 50 engineers and want a demo and pricing.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);
        List<Lead> leads = leadRepository.findAll();
        assertThat(leads).hasSize(1);
        assertThat(leads.getFirst().getTitle()).isEqualTo("Enterprise Demo - 50 engineers");
        // Model output is normalized to the values the schema allows
        assertThat(leads.getFirst().getType()).isEqualTo("pricing-inquiry");
        assertThat(leads.getFirst().getUrgency()).isEqualTo("high");
    }

    @Test
    void unknownLeadTypeFallsBackInsteadOfBreakingTheConstraint() {
        when(chatModel.call(any(Prompt.class))).thenReturn(modelReply("""
                {"isLead": true, "title": "Integration question", "type": "integration",
                 "urgency": "urgent", "summary": "Asks about an integration."}
                """));

        UUID messageId = submit("Can your product integrate with ours?");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);
        Lead lead = leadRepository.findAll().getFirst();
        assertThat(lead.getType()).isEqualTo("other");
        assertThat(lead.getUrgency()).isEqualTo("medium");
    }

    @Test
    void nonLeadIsMarkedNotALead() {
        when(chatModel.call(any(Prompt.class))).thenReturn(modelReply("""
                {"isLead": false, "title": null, "type": null, "urgency": null, "summary": null}
                """));

        UUID messageId = submit("Nice website, good job!");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.NOT_A_LEAD);
        assertThat(leadRepository.count()).isZero();
    }

    @Test
    void transientModelErrorIsRetried() {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new TransientAiException("503 Service Unavailable"))
                .thenReturn(modelReply("""
                        {"isLead": true, "title": "Demo request", "type": "demo-request",
                         "urgency": "medium", "summary": "Wants a demo."}
                        """));

        UUID messageId = submit("Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void messageIsFailedAfterRetriesAreExhausted() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new TransientAiException("503 Service Unavailable"));

        UUID messageId = submit("Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.FAILED);
        verify(chatModel, times(3)).call(any(Prompt.class));
    }

    @Test
    void permanentModelErrorFailsWithoutRetry() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new NonTransientAiException("401 Unauthorized"));

        UUID messageId = submit("Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.FAILED);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void invalidRequestIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/messages", new CreateMessageRequest("", "not-an-email", ""), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageRepository.count()).isZero();
    }

    private UUID submit(String content) {
        ResponseEntity<MessageResponse> response = restTemplate.postForEntity(
                "/api/v1/messages",
                new CreateMessageRequest("Test Sender", "sender@example.com", content),
                MessageResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().status()).isEqualTo(MessageStatus.PENDING);
        return response.getBody().id();
    }

    private Message awaitProcessed(UUID messageId) {
        return await().atMost(Duration.ofSeconds(15)).until(
                () -> messageRepository.findById(messageId).orElseThrow(),
                message -> message.getStatus() != MessageStatus.PENDING);
    }

    private static ChatResponse modelReply(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }
}
