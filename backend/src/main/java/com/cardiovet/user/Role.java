package com.cardiovet.user;

/**
 * Papeis de acesso do sistema.
 * ADMIN pode gerenciar usuarios e excluir dados sensiveis (regra de negocio de seguranca).
 * VETERINARIO realiza exames e gera laudos.
 */
public enum Role {
    ADMIN,
    VETERINARIO
}
