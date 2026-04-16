package com.fcar.domain;

import com.fcar.domain.enums.OrderStatus;
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
@Table(name = "car_orders")
public class CarOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_inventory_id", nullable = false)
    private CarInventory carInventory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.DEPOSITED;

    @Column(name = "total_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "deposit_amount", precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "paid_amount", precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "refunded_amount", precision = 18, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "refund_method", length = 50)
    private String refundMethod;

    @Column(name = "refund_bank_info", length = 255)
    private String refundBankInfo;

    /** Đường dẫn web tới ảnh chứng từ hoàn tiền (upload khi admin xác nhận REFUNDED). */
    @Column(name = "refund_proof_image_url", length = 512)
    private String refundProofImageUrl;
}

