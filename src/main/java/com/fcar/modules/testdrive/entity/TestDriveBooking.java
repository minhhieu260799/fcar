package com.fcar.modules.testdrive.entity;

import com.fcar.core.entity.BaseEntity;
import com.fcar.modules.branch.entity.Branch;
import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.testdrive.entity.enums.TestDriveStatus;
import com.fcar.modules.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "test_drive_bookings")
public class TestDriveBooking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_inventory_id", nullable = false)
    private CarInventory carInventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "test_datetime", nullable = false)
    private LocalDateTime testDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TestDriveStatus status = TestDriveStatus.PENDING;
}

