package com.cardiovet.common.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
        validator.initialize(new StrongPasswordStub());
        context = mock(ConstraintValidatorContext.class);
        builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Senha@123", "A1b!cdef", "Xyz#98765"})
    void aceitaSenhaQueCumpreAsCincoRegras(String password) {
        assertTrue(validator.isValid(password, context));
    }

    @Test
    void relataQualRegraFalhou() {
        assertFalse(validator.isValid("senha123", context));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(message.capture());
        assertTrue(message.getValue().contains("uma letra maiuscula"));
        assertTrue(message.getValue().contains("um caractere especial"));
    }

    @Test
    void relataTamanhoMinimo() {
        assertFalse(validator.isValid("A1b!", context));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(context).buildConstraintViolationWithTemplate(message.capture());
        assertTrue(message.getValue().contains("minimo de 8 caracteres"));
    }

    private record StrongPasswordStub() implements StrongPassword {

        @Override
        public String message() {
            return "Senha fora da politica";
        }

        @Override
        public int minLength() {
            return 8;
        }

        @Override
        public Class<?>[] groups() {
            return new Class<?>[0];
        }

        @Override
        public Class<? extends jakarta.validation.Payload>[] payload() {
            return newPayloadArray();
        }

        @SuppressWarnings("unchecked")
        private static Class<? extends jakarta.validation.Payload>[] newPayloadArray() {
            return (Class<? extends jakarta.validation.Payload>[]) new Class<?>[0];
        }

        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return StrongPassword.class;
        }
    }
}
