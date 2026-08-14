package com.atguigu.java.ai.langchain4j.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthCredentialsRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password
) {
    public AuthCredentialsRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
