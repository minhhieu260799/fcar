package com.fcar.modules.catalog.repository;

import com.fcar.modules.catalog.entity.Brand;
import com.fcar.modules.branch.entity.Branch;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarColor;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.catalog.entity.CarModel;
import com.fcar.modules.catalog.entity.Segment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarInventoryRepository extends JpaRepository<CarInventory, Long> {

    Optional<CarInventory> findByCarDefinitionAndBranchAndCarColor(
            CarDefinition carDefinition, Branch branch, CarColor carColor);

    List<CarInventory> findByDisabledFalse();

    List<CarInventory> findByCarDefinitionAndDisabledFalse(CarDefinition definition);

    /** Dùng cho màn liên hệ tư vấn (view cần branch; open-in-view tắt). */
    @Query("select ci from CarInventory ci join fetch ci.branch where ci.carDefinition = :definition and ci.disabled = false")
    List<CarInventory> findByCarDefinitionAndDisabledFalseFetchBranch(@Param("definition") CarDefinition definition);

    boolean existsByBranchAndDisabledFalseAndQuantityGreaterThan(Branch branch, int quantity);

    boolean existsByCarDefinition_BrandAndDisabledFalseAndQuantityGreaterThan(Brand brand, int quantity);

    boolean existsByCarDefinition_ModelAndDisabledFalseAndQuantityGreaterThan(CarModel model, int quantity);

    boolean existsByCarDefinition_SegmentAndDisabledFalseAndQuantityGreaterThan(Segment segment, int quantity);

    boolean existsByCarDefinitionAndDisabledFalseAndQuantityGreaterThan(CarDefinition definition, int quantity);

    boolean existsByCarDefinition(CarDefinition carDefinition);

    List<CarInventory> findByCarDefinition(CarDefinition carDefinition);

    @Query("select ci from CarInventory ci " +
           "join fetch ci.carDefinition d " +
           "join fetch d.brand " +
           "join fetch d.model " +
           "left join fetch d.segment " +
           "join fetch ci.carColor " +
           "left join fetch ci.branch " +
           "where (:modelId is null or d.model.id = :modelId)")
    List<CarInventory> findAllWithRelationsFiltered(@Param("modelId") Long modelId);

    @Query("select distinct m from CarModel m join fetch m.brand b " +
           "where m.id in (select d.model.id from CarInventory ci join ci.carDefinition d) " +
           "order by b.name asc, m.name asc")
    List<CarModel> findDistinctModelsInInventory();

    @Query("select ci from CarInventory ci " +
           "join fetch ci.carDefinition d " +
           "join fetch d.brand " +
           "join fetch d.model " +
           "left join fetch d.segment " +
           "join fetch ci.carColor " +
           "left join fetch ci.branch " +
           "where ci.id = :id")
    Optional<CarInventory> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT ci.carDefinition.id, SUM(ci.quantity) FROM CarInventory ci " +
           "WHERE ci.disabled = false AND ci.carDefinition.id IN :ids " +
           "GROUP BY ci.carDefinition.id")
    List<Object[]> sumQuantityByDefinitionIds(@Param("ids") Collection<Long> ids);
}


