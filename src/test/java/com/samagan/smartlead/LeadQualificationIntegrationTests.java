package com.samagan.smartlead;

import com.samagan.smartlead.lead.Lead;
import com.samagan.smartlead.message.MessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeadQualificationIntegrationTests extends IntegrationTestSupport {

    private String formKey;

    @BeforeEach
    void createAccount() {
        formKey = firstFormKey(register("owner@example.com"));
    }

    @Test
    void qualifiedMessageBecomesLead() {
        modelReplies("""
                {"isLead": true, "title": "Enterprise Demo - 50 engineers", "type": "Pricing Inquiry",
                 "urgency": "HIGH", "summary": "Team of 50 wants a demo and pricing."}
                """);

        UUID messageId = submit(formKey, "We are 50 engineers and want a demo and pricing.");

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
        modelReplies("""
                {"isLead": true, "title": "Integration question", "type": "integration",
                 "urgency": "urgent", "summary": "Asks about an integration."}
                """);

        UUID messageId = submit(formKey, "Can your product integrate with ours?");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);
        Lead lead = leadRepository.findAll().getFirst();
        assertThat(lead.getType()).isEqualTo("other");
        assertThat(lead.getUrgency()).isEqualTo("medium");
    }

    @Test
    void nonLeadIsMarkedNotALead() {
        modelReplies("""
                {"isLead": false, "title": null, "type": null, "urgency": null, "summary": null}
                """);

        UUID messageId = submit(formKey, "Nice website, good job!");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.NOT_A_LEAD);
        assertThat(leadRepository.count()).isZero();
    }

    @Test
    void transientModelErrorIsRetried() {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new TransientAiException("503 Service Unavailable"))
                .thenReturn(modelReply(LEAD_REPLY));

        UUID messageId = submit(formKey, "Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);
        verify(chatModel, times(2)).call(any(Prompt.class));
    }

    @Test
    void messageIsFailedAfterRetriesAreExhausted() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new TransientAiException("503 Service Unavailable"));

        UUID messageId = submit(formKey, "Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.FAILED);
        verify(chatModel, times(3)).call(any(Prompt.class));
    }

    @Test
    void permanentModelErrorFailsWithoutRetry() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new NonTransientAiException("401 Unauthorized"));

        UUID messageId = submit(formKey, "Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.FAILED);
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void ownerIsEmailedAboutANewLead() {
        modelReplies("""
                {"isLead": true, "title": "Enterprise Demo - 50 engineers", "type": "demo-request",
                 "urgency": "high", "summary": "Team of 50 wants a demo."}
                """);

        awaitProcessed(submit(formKey, "We are 50 engineers and want a demo."));

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(email.capture());
        assertThat(email.getValue().getTo()).containsExactly("owner@example.com");
        // Replying goes to the person who filled in the form
        assertThat(email.getValue().getReplyTo()).isEqualTo("sender@example.com");
        assertThat(email.getValue().getSubject()).isEqualTo("New lead: Enterprise Demo - 50 engineers");
        assertThat(email.getValue().getText())
                .contains("Team of 50 wants a demo.")
                .contains("We are 50 engineers and want a demo.")
                .contains("Urgency: high");
    }

    @Test
    void noEmailIsSentForANonLead() {
        modelReplies("""
                {"isLead": false, "title": null, "type": null, "urgency": null, "summary": null}
                """);

        awaitProcessed(submit(formKey, "Nice website, good job!"));

        verify(mailSender, after(500).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void failedEmailIsRetriedAndDoesNotAffectTheLead() {
        modelReplies(LEAD_REPLY);
        doThrow(new MailSendException("SMTP server unavailable"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        UUID messageId = submit(formKey, "Please book me a demo.");

        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);
        verify(mailSender, timeout(5000).times(2)).send(any(SimpleMailMessage.class));
        assertThat(leadRepository.count()).isEqualTo(1);
    }
}
