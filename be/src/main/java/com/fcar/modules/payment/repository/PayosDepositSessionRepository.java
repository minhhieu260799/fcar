package com.fcar.modules.payment.repository;

import com.fcar.modules.payment.entity.PayosDepositSession;
import com.fcar.modules.payment.entity.enums.PayosDepositSessionStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayosDepositSessionRepository extends JpaRepository<PayosDepositSession, Long> {

    @Query(value = "SELECT NEXT VALUE FOR dbo.seq_payos_order_code", nativeQuery = true)
    Integer nextPayOsOrderCode();

    Optional<PayosDepositSession> findByPayosOrderCode(Integer payosOrderCode);

    Optional<PayosDepositSession> findByPaymentLinkIdAndUser_Id(String paymentLinkId, Long userId);

    Optional<PayosDepositSession> findTopByUser_IdAndStatusOrderByIdDesc(
            Long userId, PayosDepositSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PayosDepositSession s WHERE s.payosOrderCode = :code")
    Optional<PayosDepositSession> findByPayosOrderCodeForUpdate(@Param("code") Integer code);
}
