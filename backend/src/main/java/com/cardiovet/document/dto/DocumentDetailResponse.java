package com.cardiovet.document.dto;

import com.cardiovet.document.Document;
import com.cardiovet.document.DocumentStatus;
import com.cardiovet.patient.Patient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
        String species,
        String breed,
        String sex,
        BigDecimal weightKg,
        UUID tutorId,
        String tutorName,
        String tutorCpf,
        String reportModel,
        LocalDate examDate,
        String patientAge,
        String veterinarianName,
        String findings,
        String conclusion,
        UUID uploadedById,
        String uploadedByName,
        String extractedText,
        List<DocumentFieldResponse> fields,
        List<DocumentImageResponse> images,
        OffsetDateTime createdAt) {

    public static DocumentDetailResponse from(Document d, List<DocumentImageResponse> images) {
        Patient p = d.getPatient();
        return new DocumentDetailResponse(
                d.getId(),
                d.getFileName(),
                d.getContentType(),
                d.getFileSizeBytes(),
                d.getDocumentDate(),
                d.getStatus(),
                d.getErrorMessage(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : null,
                p != null ? p.getSpecies() : null,
                p != null ? p.getBreed() : null,
                p != null ? p.getSex() : null,
                p != null ? p.getWeightKg() : null,
                p != null ? p.getTutor().getId() : null,
                p != null ? p.getTutor().getName() : null,
                p != null ? p.getTutor().getDocument() : null,
                d.getReportModel(),
                d.getExamDate(),
                d.getPatientAge(),
                d.getVeterinarianName(),
                d.getFindings(),
                d.getConclusion(),
                d.getUploadedBy().getId(),
                d.getUploadedBy().getName(),
                d.getExtractedText(),
                d.getFields().stream().map(DocumentFieldResponse::from).toList(),
                images,
                d.getCreatedAt());
    }
}
