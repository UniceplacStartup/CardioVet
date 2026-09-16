package com.cardiovet.common.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35"})
    void aceitaCpfComDigitoVerificadorCorreto(String cpf) {
        assertTrue(validator.isValid(cpf, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "12345678901", "111.111.111-11", "5299822472"})
    void rejeitaCpfInvalido(String cpf) {
        assertFalse(validator.isValid(cpf, null));
    }

    @Test
    void ignoraValorAusente() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("  ", null));
    }
}
