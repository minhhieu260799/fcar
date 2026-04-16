package com.fcar.repository;

import com.fcar.domain.OtpToken;
import com.fcar.domain.enums.OtpType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByTypeAndTargetOrderByCreatedAtDesc(OtpType type, String target);

    long countByTypeAndTargetAndCreatedAtAfter(OtpType type, String target, LocalDateTime after);
}

