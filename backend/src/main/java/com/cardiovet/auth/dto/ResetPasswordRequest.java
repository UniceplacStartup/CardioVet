package com.cardiovet.auth.dto;

import com.cardiovet.common.validation.FieldsMatch;
import com.cardiovet.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

@FieldsMatch(field = "password", confirmation = "passwordConfirm", message = "As senhas informadas nao coincidem")
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @StrongPassword String password,
        @NotBlank String passwordConfirm) {
}
