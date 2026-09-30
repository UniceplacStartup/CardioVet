package com.cardiovet.document;

import com.cardiovet.document.dto.DocumentImageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentImageRepository extends JpaRepository<DocumentImage, UUID> {

    @Query("""
            SELECT new com.cardiovet.document.dto.DocumentImageResponse(
                i.id, i.fileName, i.contentType, i.fileSizeBytes, i.createdAt)
            FROM DocumentImage i
            WHERE i.document.id = :documentId
            ORDER BY i.createdAt
            """)
    List<DocumentImageResponse> findSummariesByDocumentId(@Param("documentId") UUID documentId);

    Optional<DocumentImage> findByIdAndDocumentId(UUID id, UUID documentId);
}
