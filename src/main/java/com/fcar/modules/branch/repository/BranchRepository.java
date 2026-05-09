package com.fcar.modules.branch.repository;

import com.fcar.modules.branch.entity.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Branch> findByDeletedFalse();

    List<Branch> findAllByOrderByNameAsc();

    List<Branch> findByDeletedFalseOrderByNameAsc();
}

