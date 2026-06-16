package com.cardiovet.document;

/**
 * Estado do processamento de extracao de um documento PDF.
 * PENDENTE   -> recebido, ainda nao processado.
 * PROCESSADO -> texto e campos extraidos com sucesso.
 * ERRO       -> falha ao ler/extrair (mensagem em {@code error_message}).
 */
public enum DocumentStatus {
    PENDENTE,
    PROCESSADO,
    ERRO
}
