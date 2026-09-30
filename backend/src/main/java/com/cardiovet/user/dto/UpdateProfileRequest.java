package com.cardiovet.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 14) String cpf,
        @Size(max = 20) String phone,
        @Size(max = 30) String crmv,
        @Size(max = 100) String specialty) {
}
