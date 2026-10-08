package com.samagan.smartlead.client;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/chat/completions")
public interface HuggingFaceClient {

    @PostExchange
    HuggingFaceResponse complete(@RequestBody HuggingFaceRequest request);
}
