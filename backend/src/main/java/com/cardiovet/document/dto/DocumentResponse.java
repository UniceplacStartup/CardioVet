package com.cardiovet.document.dto;

import com.cardiovet.document.DocumentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        LocalDate documentDate,
        LocalDate examDate,
        String reportModel,
        DocumentStatus status,
        UUID patientId,
        String patientName,
        UUID uploadedById,
        String uploadedByName,
        int fieldCount,
        OffsetDateTime createdAt) {
}
