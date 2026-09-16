package com.cardiovet.common.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FieldsMatchValidatorTest {

    record Pair(String email, String emailConfirm) {}

    private FieldsMatchValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new FieldsMatchValidator();
        validator.initialize(new FieldsMatchStub("email", "emailConfirm"));
        context = mock(ConstraintValidatorContext.class);
        var builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        var nodeBuilder = mock(
                ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
    }

    @Test
    void aceitaQuandoOsValoresCoincidem() {
        assertTrue(validator.isValid(new Pair("a@b.com", "a@b.com"), context));
    }

    @Test
    void rejeitaQuandoOsValoresDiferem() {
        assertFalse(validator.isValid(new Pair("a@b.com", "c@d.com"), context));
    }

    private record FieldsMatchStub(String field, String confirmation) implements FieldsMatch {

        @Override
        public String message() {
            return "nao coincidem";
        }

        @Override
        public Class<?>[] groups() {
            return new Class<?>[0];
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends jakarta.validation.Payload>[] payload() {
            return (Class<? extends jakarta.validation.Payload>[]) new Class<?>[0];
        }

        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return FieldsMatch.class;
        }
    }
}
