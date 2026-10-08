package com.samagan.smartlead.client;


import java.util.List;

public record HuggingFaceRequest(
        String model,
        List<MessageItem> messages,
        Double temperature,
        boolean stream
) {
    public record MessageItem(String role, String content) {}

    public static HuggingFaceRequest create(String model, String systemPromt, String userMessage){
        return new HuggingFaceRequest(
                model,
                List.of(
                        new MessageItem("system", systemPromt),
                        new MessageItem("user", userMessage)
                ),
                0.1,
                false
        );
    }
}
