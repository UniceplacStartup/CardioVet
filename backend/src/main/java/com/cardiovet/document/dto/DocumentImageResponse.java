package com.cardiovet.document.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentImageResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        OffsetDateTime createdAt) {
}
