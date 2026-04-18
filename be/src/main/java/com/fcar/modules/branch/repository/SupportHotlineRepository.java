package com.fcar.modules.branch.repository;

import com.fcar.modules.branch.entity.SupportHotline;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportHotlineRepository extends JpaRepository<SupportHotline, Long> {

    List<SupportHotline> findByActiveTrue();
}

