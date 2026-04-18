package com.fcar.modules.payment.repository;

import com.fcar.modules.payment.entity.StoreBankAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreBankAccountRepository extends JpaRepository<StoreBankAccount, Long> {

    List<StoreBankAccount> findByActiveTrue();

    List<StoreBankAccount> findByActiveTrueOrderByIdAsc();
}

