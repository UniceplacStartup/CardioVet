package com.cardiovet.user;

import com.cardiovet.user.dto.ChangePasswordRequest;
import com.cardiovet.user.dto.UpdateProfileRequest;
import com.cardiovet.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Dados do usuario autenticado")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Retorna o perfil do usuario autenticado")
    @GetMapping
    public UserProfileResponse me(@AuthenticationPrincipal User user) {
        return userService.me(user);
    }

    @Operation(summary = "Atualiza o perfil do usuario autenticado")
    @PutMapping
    public UserProfileResponse update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user, request);
    }

    @Operation(summary = "Altera a senha do usuario autenticado")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }
}
