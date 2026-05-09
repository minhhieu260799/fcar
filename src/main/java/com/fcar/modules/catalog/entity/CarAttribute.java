package com.fcar.modules.catalog.entity;

import com.fcar.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "car_attributes")
public class CarAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_definition_id", nullable = false)
    private CarDefinition carDefinition;

    @Column(name = "attr_name", nullable = false, length = 50)
    private String name;

    @Column(name = "attr_value", nullable = false, length = 100)
    private String value;
}

