package com.cardiovet.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class FieldsMatchValidator implements ConstraintValidator<FieldsMatch, Object> {

    private String field;
    private String confirmation;
    private String message;

    @Override
    public void initialize(FieldsMatch annotation) {
        this.field = annotation.field();
        this.confirmation = annotation.confirmation();
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        Object left = read(value, field);
        Object right = read(value, confirmation);
        if (Objects.equals(left, right)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(confirmation)
                .addConstraintViolation();
        return false;
    }

    private Object read(Object target, String property) {
        try {
            return target.getClass().getMethod(property).invoke(target);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(
                    "Propriedade '" + property + "' inexistente em " + target.getClass().getName(), e);
        }
    }
}
