package com.fcar.modules.payment.repository;

import com.fcar.modules.payment.entity.PaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentConfigRepository extends JpaRepository<PaymentConfig, Long> {
}

