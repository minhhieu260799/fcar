package com.fcar.repository;

import com.fcar.domain.StoreBankAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreBankAccountRepository extends JpaRepository<StoreBankAccount, Long> {

    List<StoreBankAccount> findByActiveTrue();

    List<StoreBankAccount> findByActiveTrueOrderByIdAsc();
}

