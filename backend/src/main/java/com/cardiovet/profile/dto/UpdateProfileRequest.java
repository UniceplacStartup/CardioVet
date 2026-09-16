package com.cardiovet.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Pattern(
                regexp = "[^0-9]*(?:[0-9][^0-9]*){10,11}",
                message = "Informe o telefone com DDD (10 ou 11 digitos)") String phone,
        @Size(max = 100) String specialty) {
}
