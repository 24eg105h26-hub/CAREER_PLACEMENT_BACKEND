package com.smartplacement.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private Long conversationId;
    private String message;
    private String prompt; // Compatibility fallback for older clients

    public String getEffectiveMessage() {
        if (message != null && !message.isBlank()) {
            return message.trim();
        }
        if (prompt != null && !prompt.isBlank()) {
            return prompt.trim();
        }
        return "";
    }
}