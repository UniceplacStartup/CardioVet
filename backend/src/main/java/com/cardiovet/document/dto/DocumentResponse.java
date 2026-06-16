package com.cardiovet.document.dto;

import com.cardiovet.document.Document;
import com.cardiovet.document.DocumentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Resumo de um documento (sem o conteudo binario nem os campos). */
public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        LocalDate documentDate,
        DocumentStatus status,
        UUID patientId,
        String patientName,
        UUID uploadedById,
        String uploadedByName,
        int fieldCount,
        OffsetDateTime createdAt) {

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getFileName(),
                d.getContentType(),
                d.getFileSizeBytes(),
                d.getDocumentDate(),
                d.getStatus(),
                d.getPatient() != null ? d.getPatient().getId() : null,
                d.getPatient() != null ? d.getPatient().getName() : null,
                d.getUploadedBy().getId(),
                d.getUploadedBy().getName(),
                d.getFields().size(),
                d.getCreatedAt());
    }
}
