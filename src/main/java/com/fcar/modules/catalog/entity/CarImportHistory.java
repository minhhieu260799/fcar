package com.fcar.modules.catalog.entity;

import com.fcar.core.entity.BaseEntity;
import com.fcar.modules.branch.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "car_import_history")
public class CarImportHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_definition_id", nullable = false)
    private CarDefinition carDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_color_id", nullable = false)
    private CarColor carColor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "purchase_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "import_date", nullable = false)
    private LocalDate importDate;
}
