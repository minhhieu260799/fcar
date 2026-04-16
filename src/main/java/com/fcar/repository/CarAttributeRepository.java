package com.fcar.repository;

import com.fcar.domain.CarAttribute;
import com.fcar.domain.CarDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarAttributeRepository extends JpaRepository<CarAttribute, Long> {

    List<CarAttribute> findByCarDefinition(CarDefinition carDefinition);

    List<CarAttribute> findByNameIgnoreCase(String name);
}

