package com.fcar.modules.catalog.repository;

import com.fcar.modules.catalog.entity.Brand;
import com.fcar.modules.catalog.entity.CarModel;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {

    boolean existsByBrandAndName(Brand brand, String name);

    boolean existsByBrandAndNameAndIdNot(Brand brand, String name, Long id);

    List<CarModel> findByBrandAndActiveIsTrue(Brand brand);

    List<CarModel> findByDeletedFalse();

    boolean existsByBrandAndDeletedFalse(Brand brand);

    // Tất cả dòng xe (chưa xóa) của một thương hiệu, dùng cho cascade enable/disable
    List<CarModel> findByBrandAndDeletedFalse(Brand brand);

    @Query("SELECT m FROM CarModel m LEFT JOIN FETCH m.brand WHERE m.deleted = false ORDER BY m.name")
    List<CarModel> findByDeletedFalseWithBrand();

    @Query("SELECT m FROM CarModel m LEFT JOIN FETCH m.brand ORDER BY m.name")
    List<CarModel> findAllWithBrandOrderByName();

    @Query("SELECT m FROM CarModel m LEFT JOIN FETCH m.brand WHERE m.deleted = false ORDER BY m.brand.name, m.name")
    List<CarModel> findByDeletedFalseWithBrandOrderByName();

    // Các dòng xe còn sử dụng được cho dropdown (brand + model đều active, chưa xóa)
    @Query("SELECT m FROM CarModel m " +
           "JOIN m.brand b " +
           "WHERE m.deleted = false AND m.active = true " +
           "  AND b.deleted = false AND b.active = true " +
           "ORDER BY b.name, m.name")
    List<CarModel> findUsableModelsForSelection();

    @Query("SELECT m FROM CarModel m JOIN FETCH m.brand b WHERE m.id IN :ids ORDER BY b.name, m.name")
    List<CarModel> findAllByIdInWithBrand(@Param("ids") Collection<Long> ids);
}

