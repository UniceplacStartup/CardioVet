package com.cardiovet.profile;

import com.cardiovet.common.ConflictException;
import com.cardiovet.common.Digits;
import com.cardiovet.profile.dto.ChangePasswordRequest;
import com.cardiovet.profile.dto.ProfileResponse;
import com.cardiovet.profile.dto.UpdateProfileRequest;
import com.cardiovet.security.JwtService;
import com.cardiovet.user.User;
import com.cardiovet.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) {
        return ProfileResponse.from(findOrThrow(userId));
    }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        User user = findOrThrow(userId);
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ConflictException("email", "E-mail ja cadastrado");
        }

        boolean emailChanged = !user.getEmail().equals(email);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPhone(Digits.only(request.phone()));
        user.setSpecialty(request.specialty() == null || request.specialty().isBlank()
                ? null
                : request.specialty().trim());

        return ProfileResponse.from(user, emailChanged ? jwtService.generateToken(user) : null);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findOrThrow(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }
}
