package com.fcar.repository;

import com.fcar.domain.CarDefinition;
import com.fcar.domain.CarImportHistory;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarImportHistoryRepository extends JpaRepository<CarImportHistory, Long> {

    List<CarImportHistory> findByCarDefinitionOrderByImportDateDesc(CarDefinition definition);

    @Query("SELECT h FROM CarImportHistory h " +
           "JOIN FETCH h.carDefinition d " +
           "JOIN FETCH d.brand " +
           "JOIN FETCH d.model " +
           "LEFT JOIN FETCH d.segment " +
           "JOIN FETCH h.carColor " +
           "LEFT JOIN FETCH h.branch " +
           "WHERE (:filterStart IS NULL OR h.importDate >= :filterStart) AND " +
           "(:filterEnd IS NULL OR h.importDate <= :filterEnd) " +
           "ORDER BY h.importDate DESC")
    List<CarImportHistory> findAllWithRelationsFiltered(
            @Param("filterStart") LocalDate filterStart,
            @Param("filterEnd") LocalDate filterEnd);

    @Query("SELECT DISTINCT h.importDate FROM CarImportHistory h")
    List<LocalDate> findDistinctImportDates();
}

