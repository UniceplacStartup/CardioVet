package com.cardiovet.document;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByPatientId(UUID patientId, Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE (:from IS NULL OR d.documentDate >= :from)
              AND (:to   IS NULL OR d.documentDate <= :to)
            """)
    Page<Document> findByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);
}
