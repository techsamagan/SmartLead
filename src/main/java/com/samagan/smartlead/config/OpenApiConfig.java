package com.samagan.smartlead.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartLeadOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartLead Qualification API")
                        .description("Automated sales lead triage service powered by Hugging Face AI")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SmartLead Support")
                                .email("support@smartlead.io")));
    }
}