package com.cardiovet.patient.dto;

import com.cardiovet.patient.Patient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        UUID tutorId,
        String tutorName,
        String name,
        String species,
        String breed,
        String sex,
        LocalDate birthDate,
        BigDecimal weightKg,
        OffsetDateTime createdAt) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getTutor().getId(),
                patient.getTutor().getName(),
                patient.getName(),
                patient.getSpecies(),
                patient.getBreed(),
                patient.getSex(),
                patient.getBirthDate(),
                patient.getWeightKg(),
                patient.getCreatedAt());
    }
}
