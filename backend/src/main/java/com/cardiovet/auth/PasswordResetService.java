package com.cardiovet.auth;

import com.cardiovet.user.User;
import com.cardiovet.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failedAttempts = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${cardiovet.frontend-url}")
    private String frontendUrl;

    @Value("${cardiovet.security.password-reset.ttl-minutes}")
    private long ttlMinutes;

    @Value("${cardiovet.security.password-reset.log-link}")
    private boolean logLink;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase())
                .filter(User::isEnabled)
                .ifPresent(this::issueToken);
    }

    @Transactional
    public String verifyIdentity(String email, String cpf) {
        String normalizedEmail = email.trim().toLowerCase();
        Deque<Instant> attempts = failedAttempts.computeIfAbsent(normalizedEmail, k -> new ConcurrentLinkedDeque<>());
        Instant windowStart = Instant.now().minus(ATTEMPT_WINDOW);
        attempts.removeIf(at -> at.isBefore(windowStart));
        if (attempts.size() >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 15 minutos e tente novamente");
        }

        String informed = digits(cpf);
        return userRepository.findByEmail(normalizedEmail)
                .filter(User::isEnabled)
                .filter(user -> !informed.isEmpty() && informed.equals(digits(user.getCpf())))
                .map(user -> {
                    failedAttempts.remove(normalizedEmail);
                    log.info("Identidade verificada para redefinicao de senha do usuario {}", user.getId());
                    return issueToken(user);
                })
                .orElseThrow(() -> {
                    attempts.add(Instant.now());
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail e CPF nao conferem com um cadastro");
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link de redefinicao invalido ou expirado"));
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        tokenRepository.invalidateAllForUser(user.getId());
    }

    private String issueToken(User user) {
        tokenRepository.invalidateAllForUser(user.getId());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMinutes(ttlMinutes)))
                .build());
        if (logLink) {
            log.warn("[DEV] Link de redefinicao de senha: {}/redefinir-senha?token={}", frontendUrl, rawToken);
        } else {
            log.info("Redefinicao de senha solicitada para o usuario {}", user.getId());
        }
        return rawToken;
    }

    private static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
