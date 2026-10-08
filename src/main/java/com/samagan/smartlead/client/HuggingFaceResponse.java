package com.samagan.smartlead.client;

import java.util.List;

public record HuggingFaceResponse(
        List<Choice> choices
) {
    public record Choice(HfMessage message){}
    public record HfMessage(String role, String content){}

    public String content(){
        if (choices == null || choices.isEmpty() || choices.getFirst().message() == null) {
            return "";
        }
        return choices.getFirst().message().content();
    }
}
