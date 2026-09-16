package com.cardiovet.security;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID> {

    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :limit")
    int deleteExpired(@Param("limit") OffsetDateTime limit);
}
