package com.fcar.modules.catalog.entity;

import com.fcar.core.entity.BaseEntity;
import com.fcar.modules.catalog.entity.enums.BodyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "car_definitions")
public class CarDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private CarModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private Segment segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false, length = 30)
    private BodyType bodyType = BodyType.SEDAN;

    @Column(name = "production_year", nullable = false)
    private Integer productionYear;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Column(name = "seats", nullable = false)
    private Integer seats;

    @Column(name = "sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "promo_price", precision = 18, scale = 2)
    private BigDecimal promoPrice;

    @Column(name = "horsepower")
    private Integer horsepower;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "carDefinition", fetch = FetchType.LAZY)
    private List<CarImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "carDefinition", fetch = FetchType.LAZY)
    private List<CarAttribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "carDefinition", fetch = FetchType.LAZY)
    private List<CarColor> colors = new ArrayList<>();

    /**
     * Ảnh đại diện cho thẻ xe: từ {@link CarImage} gắn mẫu xe — ưu tiên {@code is_cover}, không có thì ảnh đầu tiên.
     */
    public String getCoverImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        for (CarImage img : images) {
            if (img.isCover()) {
                return img.getImageUrl();
            }
        }
        return images.get(0).getImageUrl();
    }

    /** Ảnh carousel thư viện: bỏ mọi bản ghi trùng URL với ảnh đại diện ({@link #getCoverImageUrl()}). */
    public List<CarImage> getGalleryImagesWithoutCover() {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        String coverUrl = getCoverImageUrl();
        if (coverUrl == null) {
            return List.copyOf(images);
        }
        return images.stream()
                .filter(img -> img.getImageUrl() != null && !coverUrl.equals(img.getImageUrl()))
                .toList();
    }
}

