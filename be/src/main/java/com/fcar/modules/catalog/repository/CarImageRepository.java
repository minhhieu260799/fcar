package com.fcar.modules.catalog.repository;

import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.catalog.entity.CarImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarImageRepository extends JpaRepository<CarImage, Long> {

    List<CarImage> findByCarDefinition(CarDefinition carDefinition);
}

