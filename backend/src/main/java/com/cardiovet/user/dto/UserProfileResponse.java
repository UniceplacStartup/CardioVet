package com.cardiovet.user.dto;

import com.cardiovet.user.Role;
import com.cardiovet.user.User;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        String cpf,
        String phone,
        String crmv,
        String specialty,
        Role role,
        OffsetDateTime createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getPhone(),
                user.getCrmv(),
                user.getSpecialty(),
                user.getRole(),
                user.getCreatedAt());
    }
}
