package com.fcar.domain;

import com.fcar.domain.enums.PayosDepositSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payos_deposit_sessions")
public class PayosDepositSession extends BaseEntity {

    @Column(name = "payos_order_code", nullable = false, unique = true)
    private Integer payosOrderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_definition_id", nullable = false)
    private CarDefinition carDefinition;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayosDepositSessionStatus status = PayosDepositSessionStatus.PENDING;

    @Column(name = "payment_link_id", length = 64)
    private String paymentLinkId;

    @Column(name = "checkout_url", length = 1024)
    private String checkoutUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_order_id")
    private CarOrder carOrder;
}
