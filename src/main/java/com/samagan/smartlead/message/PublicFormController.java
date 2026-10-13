package com.samagan.smartlead.message;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// The only unauthenticated write endpoint: form owners put this URL on their own websites
@RestController
@RequestMapping("/api/v1/forms/{formKey}/messages")
public class PublicFormController {

    private static final String THANK_YOU_PAGE = """
            <!doctype html>
            <html lang="en">
            <head><meta charset="utf-8"><title>Message sent</title></head>
            <body><p>Thank you, your message has been sent.</p></body>
            </html>
            """;

    private final MessageSubmissionService submissionService;

    public PublicFormController(MessageSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubmissionResponse> submitJson(@PathVariable String formKey,
                                                         @Valid @RequestBody CreateMessageRequest request,
                                                         HttpServletRequest httpRequest) {
        Message message = submissionService.submit(formKey, request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new SubmissionResponse(message.getId()));
    }

    // A plain HTML <form> posts this content type and the browser shows whatever comes back
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> submitHtmlForm(@PathVariable String formKey,
                                                 @Valid @ModelAttribute CreateMessageRequest request,
                                                 @RequestParam(name = "_gotcha", required = false) String honeypot,
                                                 HttpServletRequest httpRequest) {
        // Hidden field that people never see; only bots fill it in, so pretend it worked and drop it
        if (!StringUtils.hasText(honeypot)) {
            submissionService.submit(formKey, request, httpRequest.getRemoteAddr());
        }
        return ResponseEntity.ok(THANK_YOU_PAGE);
    }
}
