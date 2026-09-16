package com.cardiovet.security;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    @Transactional
    public void revoke(UUID jti, UUID userId, OffsetDateTime expiresAt) {
        if (jti == null || revokedTokenRepository.existsById(jti)) {
            return;
        }
        revokedTokenRepository.save(RevokedToken.builder()
                .jti(jti)
                .userId(userId)
                .expiresAt(expiresAt)
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(UUID jti) {
        return jti != null && revokedTokenRepository.existsById(jti);
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpired() {
        revokedTokenRepository.deleteExpired(OffsetDateTime.now());
    }
}
