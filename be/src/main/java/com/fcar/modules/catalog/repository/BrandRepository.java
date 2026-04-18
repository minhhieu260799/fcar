package com.fcar.modules.catalog.repository;

import com.fcar.modules.catalog.entity.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Brand> findByDeletedFalse();

    List<Brand> findAllByOrderByNameAsc();

    List<Brand> findByDeletedFalseOrderByNameAsc();

    // Các thương hiệu còn sử dụng được (chưa xóa, đang hoạt động) dùng cho dropdown
    List<Brand> findByDeletedFalseAndActiveTrueOrderByNameAsc();
}

