package com.fcar.modules.catalog.dto;

import com.fcar.modules.catalog.entity.enums.BodyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CarDefinitionEditForm {

    @NotNull
    private Long brandId;

    @NotNull
    private Long modelId;

    @NotNull
    private Long segmentId;

    @NotNull
    @Min(1900)
    private Integer productionYear;

    private BodyType bodyType;

    private String fuelType;

    @NotNull
    private Integer seats;

    @NotNull
    private BigDecimal salePrice;

    private BigDecimal promoPrice;

    private Integer horsepower;

    private String description;

    private List<String> newColors;
    private List<String> extraAttributeNames;
    private List<String> extraAttributeValues;
    private MultipartFile coverImage;
    private List<MultipartFile> galleryImages;
}
