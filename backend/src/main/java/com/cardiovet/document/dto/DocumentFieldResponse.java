package com.cardiovet.document.dto;

import com.cardiovet.document.DocumentField;

public record DocumentFieldResponse(
        String fieldKey,
        String label,
        String value,
        String unit,
        String category) {

    public static DocumentFieldResponse from(DocumentField f) {
        return new DocumentFieldResponse(f.getFieldKey(), f.getLabel(), f.getValue(), f.getUnit(), f.getCategory());
    }
}
