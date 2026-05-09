package com.fcar.modules.review.repository;

import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.order.entity.CarOrder;
import com.fcar.modules.review.entity.CarReview;
import com.fcar.modules.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarReviewRepository extends JpaRepository<CarReview, Long> {

    @Modifying
    @Query("UPDATE CarReview r SET r.carInventory = :target WHERE r.carInventory = :source")
    int reassignCarInventory(@Param("source") CarInventory source, @Param("target") CarInventory target);

    List<CarReview> findByCarInventoryAndHiddenFalse(CarInventory carInventory);

    Optional<CarReview> findByUserAndCarInventoryAndOrder(User user, CarInventory carInventory, CarOrder order);

    /**
     * Đánh giá hiển thị trên trang xe — JOIN FETCH user vì {@code spring.jpa.open-in-view=false}
     * (view cần {@code r.user.fullName}, không được lazy sau khi đóng session).
     */
    @Query("SELECT r FROM CarReview r JOIN FETCH r.user JOIN r.carInventory ci "
            + "WHERE ci.carDefinition.id = :defId AND r.hidden = false ORDER BY r.createdAt DESC")
    List<CarReview> findVisibleByCarDefinitionId(@Param("defId") Long defId);

    /** [0] = COUNT, [1] = AVG(rating) hoặc null nếu không có bản ghi. */
    @Query("SELECT COUNT(r), AVG(r.rating) FROM CarReview r JOIN r.carInventory ci "
            + "WHERE ci.carDefinition.id = :defId AND r.hidden = false")
    Object[] getAggregateStatsByCarDefinitionId(@Param("defId") Long defId);

    @Query("SELECT DISTINCT r FROM CarReview r "
            + "JOIN FETCH r.user "
            + "JOIN FETCH r.carInventory ci "
            + "JOIN FETCH ci.carDefinition d "
            + "JOIN FETCH d.brand JOIN FETCH d.model LEFT JOIN FETCH d.segment "
            + "JOIN FETCH r.order "
            + "ORDER BY r.createdAt DESC")
    List<CarReview> findAllForAdminOrderByCreatedAtDesc();
}

