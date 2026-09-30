package com.cardiovet.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportRequest(
        @Size(max = 100) String reportModel,
        @NotBlank @Size(max = 120) String patientName,
        @NotBlank @Size(max = 60) String species,
        @Size(max = 80) String breed,
        @Size(max = 10) String sex,
        @Size(max = 40) String patientAge,
        @PositiveOrZero BigDecimal weightKg,
        @NotBlank @Size(max = 150) String tutorName,
        @Size(max = 30) String tutorCpf,
        LocalDate examDate,
        LocalDate issueDate,
        @Size(max = 150) String veterinarianName,
        String findings,
        String conclusion) {
}
