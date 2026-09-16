package com.cardiovet.auth;

import com.cardiovet.auth.dto.AuthResponse;
import com.cardiovet.auth.dto.LoginRequest;
import com.cardiovet.auth.dto.RegisterRequest;
import com.cardiovet.common.ConflictException;
import com.cardiovet.common.Digits;
import com.cardiovet.security.JwtService;
import com.cardiovet.user.Role;
import com.cardiovet.user.User;
import com.cardiovet.user.UserRepository;
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

    private final UserRepository userRepository;
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
}
