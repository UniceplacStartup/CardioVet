package com.cardiovet.auth.dto;

import com.cardiovet.common.validation.Cpf;
import com.cardiovet.common.validation.FieldsMatch;
import com.cardiovet.common.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@FieldsMatch(field = "email", confirmation = "emailConfirm", message = "Os e-mails informados nao coincidem")
@FieldsMatch(field = "password", confirmation = "passwordConfirm", message = "As senhas informadas nao coincidem")
public record RegisterRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Email @Size(max = 180) String emailConfirm,
        @NotBlank @Cpf String cpf,
        @NotBlank @Pattern(
                regexp = "[^0-9]*(?:[0-9][^0-9]*){10,11}",
                message = "Informe o telefone com DDD (10 ou 11 digitos)") String phone,
        @NotBlank @Size(max = 20) String crmv,
        @Size(max = 100) String specialty,
        @NotBlank @StrongPassword String password,
        @NotBlank String passwordConfirm) {
}
