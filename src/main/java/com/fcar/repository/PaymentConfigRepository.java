package com.fcar.repository;

import com.fcar.domain.PaymentConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentConfigRepository extends JpaRepository<PaymentConfig, Long> {
}

