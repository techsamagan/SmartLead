package com.samagan.smartlead.config;

import com.samagan.smartlead.client.HuggingFaceClient;
import com.samagan.smartlead.client.HuggingFaceRequest;
import com.samagan.smartlead.client.HuggingFaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfig {

    @Value("${ai.huggingface.token:}")
    private String huggingFaceToken;

    @Value("${ai.huggingface.base-url:https://router.huggingface.co/v1}")
    private String baseUrl;

    @Bean
    public HuggingFaceClient huggingFaceClient(){
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + huggingFaceToken)
                .defaultHeader("Content-Type", "application/json")
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(HuggingFaceClient.class);
    }
}
