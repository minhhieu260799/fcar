package com.fcar.domain;

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
@Table(name = "car_inventory")
public class CarInventory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_definition_id", nullable = false)
    private CarDefinition carDefinition;

    /** Màu xe trong kho (bản ghi car_color của đúng mẫu xe). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_color_id", nullable = false)
    private CarColor carColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "disabled", nullable = false)
    private boolean disabled = false;
}
