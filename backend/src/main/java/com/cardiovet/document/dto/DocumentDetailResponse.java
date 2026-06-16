package com.cardiovet.document.dto;

import com.cardiovet.document.Document;
import com.cardiovet.document.DocumentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Detalhe de um documento incluindo os campos extraidos e o texto bruto. */
public record DocumentDetailResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        LocalDate documentDate,
        DocumentStatus status,
        String errorMessage,
        UUID patientId,
        String patientName,
        UUID uploadedById,
        String uploadedByName,
        String extractedText,
        List<DocumentFieldResponse> fields,
        OffsetDateTime createdAt) {

    public static DocumentDetailResponse from(Document d) {
        return new DocumentDetailResponse(
                d.getId(),
                d.getFileName(),
                d.getContentType(),
                d.getFileSizeBytes(),
                d.getDocumentDate(),
                d.getStatus(),
                d.getErrorMessage(),
                d.getPatient() != null ? d.getPatient().getId() : null,
                d.getPatient() != null ? d.getPatient().getName() : null,
                d.getUploadedBy().getId(),
                d.getUploadedBy().getName(),
                d.getExtractedText(),
                d.getFields().stream().map(DocumentFieldResponse::from).toList(),
                d.getCreatedAt());
    }
}
