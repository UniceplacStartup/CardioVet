package com.cardiovet.exam;

/**
 * Ciclo de vida do exame.
 * EM_ANDAMENTO -> aquisicao/medicoes; FINALIZADO -> laudo gerado (regra: laudo nao pode ser editado).
 */
public enum ExamStatus {
    EM_ANDAMENTO,
    FINALIZADO,
    CANCELADO
}
