package com.fcar.repository;

import com.fcar.domain.CarInventory;
import com.fcar.domain.CarOrder;
import com.fcar.domain.User;
import com.fcar.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarOrderRepository extends JpaRepository<CarOrder, Long> {

    @Modifying
    @Query("UPDATE CarOrder o SET o.carInventory = :target WHERE o.carInventory = :source")
    int reassignCarInventory(@Param("source") CarInventory source, @Param("target") CarInventory target);

    List<CarOrder> findByUser(User user);

    @Query("SELECT DISTINCT o FROM CarOrder o "
            + "JOIN FETCH o.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE o.user = :user")
    List<CarOrder> findByUserWithCarDetails(@Param("user") User user);

    Optional<CarOrder> findByIdAndUser(Long id, User user);

    @Query("SELECT DISTINCT o FROM CarOrder o "
            + "JOIN FETCH o.user "
            + "JOIN FETCH o.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE o.id = :id AND o.user = :user")
    Optional<CarOrder> findByIdAndUserWithCarDetails(@Param("id") Long id, @Param("user") User user);

    long countByStatus(OrderStatus status);

    List<CarOrder> findByCarInventoryAndStatusIn(CarInventory carInventory, List<OrderStatus> statuses);

    List<CarOrder> findByUserAndCarInventoryAndStatus(User user, CarInventory carInventory, OrderStatus status);

    List<CarOrder> findByUserAndCarInventoryCarDefinitionIdAndStatus(User user, Long carDefinitionId, OrderStatus status);

    /**
     * Nạp đủ quan hệ cho trang admin danh sách đơn (user, kho, mẫu xe + brand/model/segment).
     */
    @Query("SELECT DISTINCT o FROM CarOrder o "
            + "JOIN FETCH o.user "
            + "JOIN FETCH o.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment")
    List<CarOrder> findAllWithFetchGraph();

    @Query("SELECT DISTINCT o FROM CarOrder o "
            + "JOIN FETCH o.user "
            + "JOIN FETCH o.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand "
            + "JOIN FETCH d.model "
            + "LEFT JOIN FETCH d.segment "
            + "WHERE o.id = :id")
    Optional<CarOrder> findByIdWithDetailsForAdmin(@Param("id") Long id);

    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) FROM CarOrder o WHERE o.status = :status "
            + "AND o.createdAt >= :start AND o.createdAt <= :end")
    BigDecimal sumTotalPriceByStatusAndCreatedAtBetween(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(o.refundedAmount), 0) FROM CarOrder o WHERE o.status = :status "
            + "AND o.createdAt >= :start AND o.createdAt <= :end AND o.refundedAmount IS NOT NULL")
    BigDecimal sumRefundedAmountByStatusAndCreatedAtBetween(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Top mẫu xe theo số đơn DELIVERED trong khoảng (GROUP BY định nghĩa xe).
     */
    @Query("SELECT d.id, COUNT(o) FROM CarOrder o JOIN o.carInventory ci JOIN ci.carDefinition d "
            + "WHERE o.status = :status AND o.createdAt >= :start AND o.createdAt <= :end "
            + "GROUP BY d.id ORDER BY COUNT(o) DESC")
    List<Object[]> countDeliveredByDefinitionId(
            @Param("status") OrderStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);
}



