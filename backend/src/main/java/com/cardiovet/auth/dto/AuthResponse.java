package com.cardiovet.auth.dto;

import com.cardiovet.user.Role;
import java.util.UUID;

public record AuthResponse(
        String token,
        long expiresInMs,
        UUID userId,
        String name,
        String email,
        Role role) {
}
