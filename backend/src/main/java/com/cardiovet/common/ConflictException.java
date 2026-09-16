package com.cardiovet.common;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {

    private final String field;

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }
}
