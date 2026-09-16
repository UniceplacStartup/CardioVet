package com.cardiovet.auth;

import com.cardiovet.auth.dto.AuthResponse;
import com.cardiovet.auth.dto.ForgotPasswordRequest;
import com.cardiovet.auth.dto.LoginRequest;
import com.cardiovet.auth.dto.MessageResponse;
import com.cardiovet.auth.dto.RegisterRequest;
import com.cardiovet.auth.dto.ResetPasswordRequest;
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
@Tag(name = "Autenticacao", description = "Cadastro, login e redefinicao de senha")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Cadastra um novo veterinario e retorna um token JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuario existente e retorna um token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita o envio das instrucoes de redefinicao de senha")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted().body(new MessageResponse(
                "Se o e-mail estiver cadastrado, as instrucoes de redefinicao foram enviadas"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha a partir de um token valido")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso"));
    }
}
