package com.cardiovet.auth;

import com.cardiovet.auth.dto.AuthResponse;
import com.cardiovet.auth.dto.LoginRequest;
import com.cardiovet.auth.dto.RegisterRequest;
import com.cardiovet.security.JwtService;
import com.cardiovet.user.Role;
import com.cardiovet.user.User;
import com.cardiovet.user.UserRepository;
import com.cardiovet.user.dto.UserProfileResponse;
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
    public UserProfileResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail ja cadastrado");
        }
        User user = User.builder()
                .name(request.name())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .cpf(blankToNull(request.cpf()))
                .phone(blankToNull(request.phone()))
                .crmv(blankToNull(request.crmv()))
                .specialty(blankToNull(request.specialty()))
                .role(Role.VETERINARIO)
                .active(true)
                .build();
        return UserProfileResponse.from(userRepository.save(user));
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
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                jwtService.getExpirationMs(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
