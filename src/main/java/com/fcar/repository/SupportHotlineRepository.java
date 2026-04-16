package com.fcar.repository;

import com.fcar.domain.SupportHotline;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportHotlineRepository extends JpaRepository<SupportHotline, Long> {

    List<SupportHotline> findByActiveTrue();
}

