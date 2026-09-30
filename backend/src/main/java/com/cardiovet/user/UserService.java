package com.cardiovet.user;

import com.cardiovet.user.dto.ChangePasswordRequest;
import com.cardiovet.user.dto.UpdateProfileRequest;
import com.cardiovet.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse me(User principal) {
        return UserProfileResponse.from(reload(principal));
    }

    @Transactional
    public UserProfileResponse updateProfile(User principal, UpdateProfileRequest request) {
        User user = reload(principal);
        user.setName(request.name().trim());
        user.setCpf(blankToNull(request.cpf()));
        user.setPhone(blankToNull(request.phone()));
        user.setCrmv(blankToNull(request.crmv()));
        user.setSpecialty(blankToNull(request.specialty()));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changePassword(User principal, ChangePasswordRequest request) {
        User user = reload(principal);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User reload(User principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
