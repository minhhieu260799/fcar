package com.fcar.modules.catalog.repository;

import com.fcar.modules.catalog.entity.CarModel;
import com.fcar.modules.catalog.entity.Segment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SegmentRepository extends JpaRepository<Segment, Long> {

    boolean existsByModelAndName(CarModel model, String name);

    boolean existsByModelAndNameAndIdNot(CarModel model, String name, Long id);

    List<Segment> findByDeletedFalse();

    List<Segment> findAllByOrderByNameAsc();

    boolean existsByModelAndDeletedFalse(CarModel model);

    List<Segment> findByDeletedFalseOrderByNameAsc();

    // Các phiên bản (chưa xóa) của một dòng xe, dùng cho cascade enable/disable
    List<Segment> findByModelAndDeletedFalse(CarModel model);

    @Query("SELECT s FROM Segment s LEFT JOIN FETCH s.model m LEFT JOIN FETCH m.brand WHERE s.deleted = false ORDER BY m.brand.name, m.name, s.name")
    List<Segment> findByDeletedFalseWithModelAndBrandOrderByNameAsc();

    // Các phiên bản còn sử dụng được cho dropdown (brand + model + segment đều active, chưa xóa)
    @Query("SELECT s FROM Segment s " +
           "JOIN s.model m " +
           "JOIN m.brand b " +
           "WHERE s.deleted = false AND s.active = true " +
           "  AND m.deleted = false AND m.active = true " +
           "  AND b.deleted = false AND b.active = true " +
           "ORDER BY b.name, m.name, s.name")
    List<Segment> findUsableSegmentsForSelection();

    @Query(
            "SELECT s FROM Segment s JOIN FETCH s.model m JOIN FETCH m.brand b WHERE s.id IN :ids ORDER BY b.name, m.name, s.name")
    List<Segment> findAllByIdInWithModelAndBrand(@Param("ids") Collection<Long> ids);
}

