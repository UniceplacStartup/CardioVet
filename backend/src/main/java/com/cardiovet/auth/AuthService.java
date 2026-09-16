package com.cardiovet.auth;

import com.cardiovet.auth.dto.AuthResponse;
import com.cardiovet.auth.dto.ForgotPasswordRequest;
import com.cardiovet.auth.dto.LoginRequest;
import com.cardiovet.auth.dto.RegisterRequest;
import com.cardiovet.auth.dto.ResetPasswordRequest;
import com.cardiovet.common.ConflictException;
import com.cardiovet.common.Digits;
import com.cardiovet.security.JwtService;
import com.cardiovet.user.Role;
import com.cardiovet.user.User;
import com.cardiovet.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetMailer passwordResetMailer;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String cpf = Digits.only(request.cpf());
        String crmv = request.crmv().trim().toUpperCase();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email", "E-mail ja cadastrado");
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new ConflictException("cpf", "CPF ja cadastrado");
        }
        if (userRepository.existsByCrmv(crmv)) {
            throw new ConflictException("crmv", "CRMV ja cadastrado");
        }

        User user = userRepository.save(User.builder()
                .name(request.name().trim())
                .email(email)
                .cpf(cpf)
                .phone(Digits.only(request.phone()))
                .crmv(crmv)
                .specialty(trimToNull(request.specialty()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.VETERINARIO)
                .active(true)
                .build());

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));
        return buildResponse(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> user = userRepository.findByEmail(request.email().trim().toLowerCase());
        if (user.isEmpty() || !user.get().isEnabled()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        passwordResetTokenRepository.invalidateActiveTokens(user.get().getId(), now);

        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.get().getId())
                .tokenHash(sha256(token))
                .expiresAt(now.plus(RESET_TOKEN_TTL))
                .build());

        passwordResetMailer.send(user.get().getEmail(), user.get().getName(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(sha256(request.token()))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Token de redefinicao invalido ou expirado"));

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Token de redefinicao invalido ou expirado"));

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        token.setUsedAt(now);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                jwtService.getExpirationMs(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
