package com.fcar.modules.contact.repository;

import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.contact.entity.ContactRequest;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.contact.entity.enums.ContactStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {

    @Modifying
    @Query("UPDATE ContactRequest c SET c.carInventory = :target WHERE c.carInventory = :source")
    int reassignCarInventory(@Param("source") CarInventory source, @Param("target") CarInventory target);

    List<ContactRequest> findByUser(User user);

    @Query("SELECT DISTINCT c FROM ContactRequest c "
            + "JOIN FETCH c.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE c.user = :user")
    List<ContactRequest> findByUserWithCarDetails(@Param("user") User user);

    long countByStatus(ContactStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<ContactRequest> findFirstByUserAndCarInventoryAndStatus(User user, CarInventory carInventory, ContactStatus status);

    /** Admin danh sách: cần user + xe (open-in-view tắt). */
    @Query("SELECT DISTINCT c FROM ContactRequest c "
            + "JOIN FETCH c.user "
            + "JOIN FETCH c.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment")
    List<ContactRequest> findAllWithUserAndCarDetails();

    @Query("SELECT DISTINCT c FROM ContactRequest c "
            + "JOIN FETCH c.user "
            + "JOIN FETCH c.carInventory ci "
            + "LEFT JOIN FETCH ci.branch "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE c.id = :id")
    Optional<ContactRequest> findByIdWithUserAndCarDetails(@Param("id") Long id);
}

