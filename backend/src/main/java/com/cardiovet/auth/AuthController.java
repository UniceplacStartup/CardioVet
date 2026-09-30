package com.cardiovet.auth;

import com.cardiovet.auth.dto.AuthResponse;
import com.cardiovet.auth.dto.ForgotPasswordRequest;
import com.cardiovet.auth.dto.LoginRequest;
import com.cardiovet.auth.dto.RegisterRequest;
import com.cardiovet.auth.dto.ResetPasswordRequest;
import com.cardiovet.auth.dto.ResetTokenResponse;
import com.cardiovet.auth.dto.VerifyIdentityRequest;
import com.cardiovet.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Cadastro e login de usuarios (veterinarios/administradores)")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Cadastra um novo usuario; o acesso exige login em seguida")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuario existente e retorna um token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Gera um link de redefinicao de senha para o e-mail informado")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-identity")
    @Operation(summary = "Confere e-mail e CPF e retorna um token de redefinicao de senha")
    public ResetTokenResponse verifyIdentity(@Valid @RequestBody VerifyIdentityRequest request) {
        return new ResetTokenResponse(passwordResetService.verifyIdentity(request.email(), request.cpf()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha a partir do token recebido no link")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
