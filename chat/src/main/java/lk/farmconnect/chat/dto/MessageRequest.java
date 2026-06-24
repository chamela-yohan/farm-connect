package lk.farmconnect.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(
        @NotBlank(message = "Message content cannot be empty")
        String content
) {}