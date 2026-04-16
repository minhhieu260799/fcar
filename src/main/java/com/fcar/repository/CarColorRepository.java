package com.fcar.repository;

import com.fcar.domain.CarColor;
import com.fcar.domain.CarDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarColorRepository extends JpaRepository<CarColor, Long> {

    List<CarColor> findByCarDefinition(CarDefinition carDefinition);

    List<CarColor> findByCarDefinitionIn(List<CarDefinition> carDefinitions);

    /** Khớp màu đã khai báo cho mẫu xe (bỏ qua hoa/thường). */
    Optional<CarColor> findFirstByCarDefinitionAndColorValueIgnoreCase(CarDefinition carDefinition, String colorValue);
}
