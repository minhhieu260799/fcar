package com.fcar.repository;

import com.fcar.domain.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Branch> findByDeletedFalse();

    List<Branch> findAllByOrderByNameAsc();

    List<Branch> findByDeletedFalseOrderByNameAsc();
}

