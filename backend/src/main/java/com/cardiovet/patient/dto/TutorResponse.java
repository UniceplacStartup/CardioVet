package com.cardiovet.patient.dto;

import com.cardiovet.patient.Tutor;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TutorResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String document,
        OffsetDateTime createdAt) {

    public static TutorResponse from(Tutor tutor) {
        return new TutorResponse(
                tutor.getId(),
                tutor.getName(),
                tutor.getEmail(),
                tutor.getPhone(),
                tutor.getDocument(),
                tutor.getCreatedAt());
    }
}
