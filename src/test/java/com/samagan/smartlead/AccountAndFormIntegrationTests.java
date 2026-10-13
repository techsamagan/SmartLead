package com.samagan.smartlead;

import com.fasterxml.jackson.databind.JsonNode;
import com.samagan.smartlead.message.MessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccountAndFormIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrationCreatesAnAccountWithOneForm() {
        String token = register("Owner@Example.com");

        JsonNode forms = get("/api/v1/forms", token).getBody();
        assertThat(forms).hasSize(1);
        assertThat(forms.get(0).get("formKey").asText()).hasSize(32);
        // Emails are stored lowercased so login is case-insensitive
        assertThat(userRepository.findByEmail("owner@example.com")).isPresent();
    }

    @Test
    void duplicateEmailIsRejected() {
        register("owner@example.com");

        ResponseEntity<JsonNode> second = restTemplate.postForEntity(
                "/api/v1/auth/register", Map.of("email", "OWNER@example.com", "password", PASSWORD), JsonNode.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void weakPasswordIsRejected() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/auth/register", Map.of("email", "owner@example.com", "password", "short"), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void loginReturnsAWorkingTokenAndRejectsAWrongPassword() {
        register("owner@example.com");

        ResponseEntity<JsonNode> login = restTemplate.postForEntity(
                "/api/v1/auth/login", Map.of("email", "owner@example.com", "password", PASSWORD), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/forms", login.getBody().get("token").asText()).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> wrong = restTemplate.postForEntity(
                "/api/v1/auth/login", Map.of("email", "owner@example.com", "password", "wrong-password"), JsonNode.class);
        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void dashboardEndpointsRequireAToken() {
        for (String path : new String[]{"/api/v1/forms", "/api/v1/messages", "/api/v1/leads"}) {
            assertThat(get(path, null).getStatusCode()).as(path).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(get(path, "not-a-real-token").getStatusCode()).as(path).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void usersOnlySeeTheirOwnMessagesAndLeads() {
        modelReplies(LEAD_REPLY);
        String alice = register("alice@example.com");
        String bob = register("bob@example.com");

        UUID messageId = submit(firstFormKey(alice), "Please book me a demo.");
        assertThat(awaitProcessed(messageId).getStatus()).isEqualTo(MessageStatus.QUALIFIED);

        JsonNode aliceLeads = get("/api/v1/leads", alice).getBody().get("content");
        assertThat(aliceLeads).hasSize(1);
        assertThat(aliceLeads.get(0).get("senderEmail").asText()).isEqualTo("sender@example.com");
        assertThat(get("/api/v1/messages", alice).getBody().get("content")).hasSize(1);

        assertThat(get("/api/v1/leads", bob).getBody().get("content")).isEmpty();
        assertThat(get("/api/v1/messages", bob).getBody().get("content")).isEmpty();

        // Another user's ids look like they do not exist
        String leadId = aliceLeads.get(0).get("id").asText();
        assertThat(get("/api/v1/leads/" + leadId, alice).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/leads/" + leadId, bob).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        String aliceFormId = get("/api/v1/forms", alice).getBody().get(0).get("id").asText();
        ResponseEntity<Void> delete = restTemplate.exchange(
                "/api/v1/forms/" + aliceFormId, HttpMethod.DELETE, new HttpEntity<>(bearer(bob)), Void.class);
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(formRepository.count()).isEqualTo(2);
    }

    @Test
    void leadsCanBeFilteredByUrgency() {
        modelReplies(LEAD_REPLY);
        String token = register("owner@example.com");
        awaitProcessed(submit(firstFormKey(token), "Please book me a demo."));

        assertThat(get("/api/v1/leads?urgency=medium", token).getBody().get("content")).hasSize(1);
        assertThat(get("/api/v1/leads?urgency=high", token).getBody().get("content")).isEmpty();
        assertThat(get("/api/v1/leads?type=demo-request&urgency=medium", token).getBody().get("content")).hasSize(1);
    }

    @Test
    void deletingAFormRemovesItsMessagesAndLeads() {
        modelReplies(LEAD_REPLY);
        String token = register("owner@example.com");
        awaitProcessed(submit(firstFormKey(token), "Please book me a demo."));
        String formId = get("/api/v1/forms", token).getBody().get(0).get("id").asText();

        ResponseEntity<Void> delete = restTemplate.exchange(
                "/api/v1/forms/" + formId, HttpMethod.DELETE, new HttpEntity<>(bearer(token)), Void.class);

        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(messageRepository.count()).isZero();
        assertThat(leadRepository.count()).isZero();
    }

    @Test
    void plainHtmlFormPostIsAccepted() {
        modelReplies(LEAD_REPLY);
        String formKey = firstFormKey(register("owner@example.com"));

        ResponseEntity<String> response = postHtmlForm(formKey, "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Thank you");
        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    void honeypotSubmissionIsSilentlyDropped() {
        String formKey = firstFormKey(register("owner@example.com"));

        ResponseEntity<String> response = postHtmlForm(formKey, "http://spam.example");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void unknownFormKeyIsNotFound() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/forms/doesnotexist/messages",
                Map.of("senderName", "Test", "senderEmail", "sender@example.com", "content", "Hello"),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidSubmissionIsRejected() {
        String formKey = firstFormKey(register("owner@example.com"));

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/v1/forms/" + formKey + "/messages",
                Map.of("senderName", "", "senderEmail", "not-an-email", "content", ""),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(messageRepository.count()).isZero();
    }

    @Test
    void submissionsAreRateLimitedPerForm() {
        modelReplies(LEAD_REPLY);
        String formKey = firstFormKey(register("owner@example.com"));
        Map<String, String> body = Map.of("senderName", "Test", "senderEmail", "sender@example.com", "content", "Hello");
        String path = "/api/v1/forms/" + formKey + "/messages";

        for (int i = 0; i < 10; i++) {
            assertThat(restTemplate.postForEntity(path, body, JsonNode.class).getStatusCode())
                    .isEqualTo(HttpStatus.ACCEPTED);
        }
        assertThat(restTemplate.postForEntity(path, body, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void anyWebsiteMayCallTheSubmissionEndpoint() throws Exception {
        mockMvc.perform(options("/api/v1/forms/anykey/messages")
                        .header(HttpHeaders.ORIGIN, "https://customer-site.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
    }

    @Test
    void otherWebsitesMayNotCallTheDashboardApi() throws Exception {
        mockMvc.perform(options("/api/v1/leads")
                        .header(HttpHeaders.ORIGIN, "https://customer-site.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private ResponseEntity<String> postHtmlForm(String formKey, String honeypot) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> fields = new LinkedMultiValueMap<>();
        fields.add("senderName", "Test Sender");
        fields.add("senderEmail", "sender@example.com");
        fields.add("content", "Please book me a demo.");
        fields.add("_gotcha", honeypot);
        return restTemplate.postForEntity(
                "/api/v1/forms/" + formKey + "/messages", new HttpEntity<>(fields, headers), String.class);
    }
}
