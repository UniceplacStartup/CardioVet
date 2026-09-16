package com.cardiovet.common;

public final class Digits {

    private Digits() {
    }

    public static String only(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
