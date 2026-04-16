package com.fcar.repository;

import com.fcar.domain.CarDefinition;
import com.fcar.domain.CarImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarImageRepository extends JpaRepository<CarImage, Long> {

    List<CarImage> findByCarDefinition(CarDefinition carDefinition);
}

