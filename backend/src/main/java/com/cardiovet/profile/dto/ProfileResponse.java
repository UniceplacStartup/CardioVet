package com.cardiovet.profile.dto;

import com.cardiovet.user.Role;
import com.cardiovet.user.User;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        String crmv,
        String specialty,
        Role role,
        OffsetDateTime createdAt,
        String token) {

    public static ProfileResponse from(User user) {
        return from(user, null);
    }

    public static ProfileResponse from(User user, String token) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getPhone(),
                user.getCrmv(),
                user.getSpecialty(),
                user.getRole(),
                user.getCreatedAt(),
                token);
    }
}
