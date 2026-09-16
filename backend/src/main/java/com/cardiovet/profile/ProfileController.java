package com.cardiovet.profile;

import com.cardiovet.profile.dto.ChangePasswordRequest;
import com.cardiovet.profile.dto.ProfileResponse;
import com.cardiovet.profile.dto.UpdateProfileRequest;
import com.cardiovet.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Dados do usuario autenticado")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Retorna os dados do usuario autenticado")
    public ProfileResponse get(@AuthenticationPrincipal User user) {
        return profileService.get(user.getId());
    }

    @PutMapping
    @Operation(summary = "Atualiza nome, e-mail, telefone e especialidade do usuario autenticado")
    public ProfileResponse update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(user.getId(), request);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Altera a senha do usuario autenticado")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
