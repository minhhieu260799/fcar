package com.fcar.repository;

import com.fcar.domain.Brand;
import com.fcar.domain.CarDefinition;
import com.fcar.domain.CarModel;
import com.fcar.domain.Segment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarDefinitionRepository extends JpaRepository<CarDefinition, Long>, JpaSpecificationExecutor<CarDefinition> {

    Optional<CarDefinition> findByBrandAndModelAndSegmentAndProductionYear(
            Brand brand,
            CarModel model,
            Segment segment,
            Integer productionYear
    );

    @Query("select d from CarDefinition d " +
           "join fetch d.brand " +
           "join fetch d.model " +
           "left join fetch d.segment")
    List<CarDefinition> findAllWithRelations();

    @Query("select d from CarDefinition d " +
           "join fetch d.brand " +
           "join fetch d.model " +
           "left join fetch d.segment " +
           "where d.deleted = false")
    List<CarDefinition> findActiveWithRelations();

    @Query("select d from CarDefinition d " +
           "join fetch d.brand " +
           "join fetch d.model " +
           "left join fetch d.segment " +
           "where d.id = :id")
    Optional<CarDefinition> findByIdWithRelations(@Param("id") Long id);

    /**
     * Chi tiết storefront: brand/model/segment + ảnh.
     * Không fetch {@code colors}/{@code attributes} cùng lúc với {@code images} (bag → MultipleBagFetchException).
     * Màu và thông số khác được nạp trong {@link com.fcar.service.CarQueryService#findListedDefinitionForDetail}.
     */
    @Query("SELECT DISTINCT d FROM CarDefinition d " +
           "JOIN FETCH d.brand " +
           "JOIN FETCH d.model " +
           "LEFT JOIN FETCH d.segment " +
           "LEFT JOIN FETCH d.images " +
           "WHERE d.id = :id")
    Optional<CarDefinition> findByIdWithDetailsForStorefront(@Param("id") Long id);

    @Query(
            value = """
                    SELECT d.id FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                    ORDER BY d.brand.name ASC, d.model.name ASC, d.productionYear ASC
                    """,
            countQuery = """
                    SELECT count(d) FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                    """
    )
    Page<Long> findListedIds(Pageable pageable);

    /**
     * Niêm yết giống {@link #findListedIds(Pageable)}, lọc theo chuỗi (hãng, dòng, phân khúc, năm SX).
     * Tham số {@code q} dạng {@code %từ khóa%}, chữ thường (xử lý ở service).
     */
    @Query(
            value = """
                    SELECT d.id FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                      AND (
                          LOWER(d.brand.name) LIKE :q
                          OR LOWER(d.model.name) LIKE :q
                          OR LOWER(COALESCE(d.segment.name, '')) LIKE :q
                          OR CAST(d.productionYear AS string) LIKE :q
                      )
                    ORDER BY d.brand.name ASC, d.model.name ASC, d.productionYear ASC
                    """,
            countQuery = """
                    SELECT count(d) FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                      AND (
                          LOWER(d.brand.name) LIKE :q
                          OR LOWER(d.model.name) LIKE :q
                          OR LOWER(COALESCE(d.segment.name, '')) LIKE :q
                          OR CAST(d.productionYear AS string) LIKE :q
                      )
                    """
    )
    Page<Long> findListedIdsBySearch(@Param("q") String q, Pageable pageable);

    @Query(
            """
                    SELECT DISTINCT d.brand.id FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                    """)
    List<Long> findDistinctListedBrandIds();

    @Query(
            """
                    SELECT DISTINCT d.model.id FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                    """)
    List<Long> findDistinctListedModelIds();

    @Query(
            """
                    SELECT DISTINCT d.segment.id FROM CarDefinition d
                    WHERE d.deleted = false AND d.active = true
                      AND d.brand.active = true AND d.brand.deleted = false
                      AND d.model.active = true AND d.model.deleted = false
                      AND (d.segment IS NULL OR (d.segment.active = true AND d.segment.deleted = false))
                      AND d.segment IS NOT NULL
                    """)
    List<Long> findDistinctListedSegmentIds();

    @Query("SELECT DISTINCT d FROM CarDefinition d " +
           "JOIN FETCH d.brand " +
           "JOIN FETCH d.model " +
           "LEFT JOIN FETCH d.segment " +
           "LEFT JOIN FETCH d.images " +
           "WHERE d.id IN :ids")
    List<CarDefinition> findListedByIdsWithImages(@Param("ids") Collection<Long> ids);

    /** Nạp brand/model/segment cho báo cáo (không lọc niêm yết). */
    @Query("SELECT DISTINCT d FROM CarDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE d.id IN :ids")
    List<CarDefinition> findByIdsWithBrandModelSegment(@Param("ids") Collection<Long> ids);

    boolean existsBySegmentAndDeletedFalse(Segment segment);

    boolean existsByModelAndDeletedFalse(CarModel model);

    // Các mẫu xe (chưa xóa) dùng cho cascade enable/disable
    List<CarDefinition> findByBrandAndDeletedFalse(Brand brand);

    List<CarDefinition> findByModelAndDeletedFalse(CarModel model);

    List<CarDefinition> findBySegmentAndDeletedFalse(Segment segment);

    @Query("select d from CarDefinition d " +
           "join fetch d.brand " +
           "join fetch d.model " +
           "left join fetch d.segment " +
           "where d.deleted = false ORDER BY d.brand.name, d.model.name, d.segment.name, d.productionYear")
    List<CarDefinition> findByDeletedFalseWithRelations();
}

