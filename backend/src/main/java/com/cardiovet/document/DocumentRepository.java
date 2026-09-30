package com.cardiovet.document;

import com.cardiovet.document.dto.DocumentResponse;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    String SELECT_SUMMARY = """
            SELECT new com.cardiovet.document.dto.DocumentResponse(
                d.id, d.fileName, d.contentType, d.fileSizeBytes, d.documentDate, d.examDate,
                d.reportModel, d.status, p.id, p.name, u.id, u.name, size(d.fields), d.createdAt)
            FROM Document d
            LEFT JOIN d.patient p
            JOIN d.uploadedBy u
            """;

    @Query(value = SELECT_SUMMARY + "WHERE d.documentDate BETWEEN :from AND :to",
            countQuery = "SELECT count(d) FROM Document d WHERE d.documentDate BETWEEN :from AND :to")
    Page<DocumentResponse> search(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query(value = SELECT_SUMMARY + "WHERE d.documentDate BETWEEN :from AND :to AND p.id = :patientId",
            countQuery = """
            SELECT count(d) FROM Document d
            WHERE d.documentDate BETWEEN :from AND :to AND d.patient.id = :patientId
            """)
    Page<DocumentResponse> searchByPatient(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("patientId") UUID patientId,
            Pageable pageable);
}
