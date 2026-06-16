package com.cardiovet.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PatientRequest(
        @NotNull UUID tutorId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String species,
        @Size(max = 80) String breed,
        @Size(max = 10) String sex,
        LocalDate birthDate,
        @PositiveOrZero BigDecimal weightKg) {
}
