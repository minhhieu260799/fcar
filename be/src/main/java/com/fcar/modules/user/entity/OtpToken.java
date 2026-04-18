package com.fcar.modules.user.entity;

import com.fcar.core.entity.BaseEntity;
import com.fcar.modules.user.entity.enums.OtpType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_tokens")
public class OtpToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private OtpType type;

    @Column(name = "target", nullable = false, length = 150)
    private String target;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "send_count", nullable = false)
    private int sendCount;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "used", nullable = false)
    private boolean used = false;
}

