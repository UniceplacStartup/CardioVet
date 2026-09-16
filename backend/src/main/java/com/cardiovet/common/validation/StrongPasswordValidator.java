package com.cardiovet.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int minLength;

    @Override
    public void initialize(StrongPassword annotation) {
        this.minLength = annotation.minLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        List<String> missing = new ArrayList<>();
        if (value.length() < minLength) {
            missing.add("minimo de " + minLength + " caracteres");
        }
        if (value.chars().noneMatch(Character::isUpperCase)) {
            missing.add("uma letra maiuscula");
        }
        if (value.chars().noneMatch(Character::isLowerCase)) {
            missing.add("uma letra minuscula");
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            missing.add("um numero");
        }
        if (value.chars().allMatch(Character::isLetterOrDigit)) {
            missing.add("um caractere especial");
        }

        if (missing.isEmpty()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("A senha precisa conter " + String.join(", ", missing))
                .addConstraintViolation();
        return false;
    }
}
