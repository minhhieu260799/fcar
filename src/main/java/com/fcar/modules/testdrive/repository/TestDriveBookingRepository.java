package com.fcar.modules.testdrive.repository;

import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.testdrive.entity.TestDriveBooking;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.testdrive.entity.enums.TestDriveStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestDriveBookingRepository extends JpaRepository<TestDriveBooking, Long> {

    @Modifying
    @Query("UPDATE TestDriveBooking t SET t.carInventory = :target WHERE t.carInventory = :source")
    int reassignCarInventory(@Param("source") CarInventory source, @Param("target") CarInventory target);

    List<TestDriveBooking> findByUser(User user);

    @Query("SELECT DISTINCT t FROM TestDriveBooking t "
            + "JOIN FETCH t.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE t.user = :user")
    List<TestDriveBooking> findByUserWithCarDetails(@Param("user") User user);

    long countByStatus(TestDriveStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<TestDriveBooking> findByCarInventoryAndTestDateTime(CarInventory carInventory, LocalDateTime testDateTime);

    List<TestDriveBooking> findByUserAndCarInventory(User user, CarInventory carInventory);

    /** Admin danh sách: user + xe (open-in-view tắt). */
    @Query("SELECT DISTINCT t FROM TestDriveBooking t "
            + "JOIN FETCH t.user "
            + "JOIN FETCH t.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment")
    List<TestDriveBooking> findAllWithUserAndCarDetails();

    @Query("SELECT DISTINCT t FROM TestDriveBooking t "
            + "JOIN FETCH t.user "
            + "JOIN FETCH t.carInventory ci "
            + "LEFT JOIN FETCH ci.branch "
            + "LEFT JOIN FETCH t.branch "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE t.id = :id")
    Optional<TestDriveBooking> findByIdWithUserAndCarDetails(@Param("id") Long id);
}

