package com.cardiovet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyIdentityRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 14) String cpf) {
}
