package com.smartplacement.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long conversationId;
    private String reply;
    private String answer; // Compatibility alias for reply
    private List<String> sources;

    public static ChatResponse of(Long conversationId, String reply, List<String> sources) {
        return ChatResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .answer(reply)
                .sources(sources != null ? sources : List.of())
                .build();
    }
}