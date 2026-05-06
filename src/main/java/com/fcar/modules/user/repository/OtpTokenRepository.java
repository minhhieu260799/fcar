package com.fcar.modules.user.repository;

import com.fcar.modules.user.entity.OtpToken;
import com.fcar.modules.user.entity.enums.OtpType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    Optional<OtpToken> findTopByTypeAndTargetOrderByCreatedAtDesc(OtpType type, String target);

    long countByTypeAndTargetAndCreatedAtAfter(OtpType type, String target, LocalDateTime after);
}

