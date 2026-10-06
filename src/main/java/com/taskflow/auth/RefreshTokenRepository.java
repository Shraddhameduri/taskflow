package com.taskflow.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    java.util.List<RefreshToken> findByUsernameAndRevokedFalse(String username);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < current_timestamp or t.revoked = true")
    int deleteExpiredOrRevoked();
}
