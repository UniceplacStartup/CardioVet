package com.cardiovet.auth;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingPasswordResetMailer implements PasswordResetMailer {

    private final boolean exposeToken;

    public LoggingPasswordResetMailer(
            @Value("${cardiovet.security.password-reset.expose-token:false}") boolean exposeToken) {
        this.exposeToken = exposeToken;
    }

    @PostConstruct
    void warnWhenTokenIsExposed() {
        if (exposeToken) {
            log.warn("cardiovet.security.password-reset.expose-token esta ATIVO: "
                    + "tokens de redefinicao serao gravados em texto puro no log. Nunca use em producao.");
        }
    }

    @Override
    public void send(String email, String name, String token) {
        if (exposeToken) {
            log.warn("[reset-senha] destinatario={} token={}", email, token);
            return;
        }
        log.info("[reset-senha] solicitacao registrada para destinatario={} tokenRef={}",
                mask(email), AuthService.sha256(token).substring(0, 8));
    }

    private String mask(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
