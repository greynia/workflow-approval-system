package com.eva.workflow.approval.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @Pattern(regexp = "^(en|zh-TW)$") String preferredLocale
) {
}
