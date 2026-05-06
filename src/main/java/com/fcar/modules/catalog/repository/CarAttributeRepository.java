package com.fcar.modules.catalog.repository;

import com.fcar.modules.catalog.entity.CarAttribute;
import com.fcar.modules.catalog.entity.CarDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarAttributeRepository extends JpaRepository<CarAttribute, Long> {

    List<CarAttribute> findByCarDefinition(CarDefinition carDefinition);

    List<CarAttribute> findByNameIgnoreCase(String name);
}

