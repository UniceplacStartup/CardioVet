package com.cardiovet.profile.dto;

import com.cardiovet.common.validation.FieldsMatch;
import com.cardiovet.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

@FieldsMatch(field = "newPassword", confirmation = "newPasswordConfirm",
        message = "As senhas informadas nao coincidem")
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword String newPassword,
        @NotBlank String newPasswordConfirm) {
}
