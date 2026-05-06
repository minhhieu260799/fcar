package com.fcar.modules.favorite.repository;

import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.favorite.entity.Favorite;
import com.fcar.modules.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser(User user);

    /** ID mẫu xe (definition) user đã yêu thích — dùng khi OSIV tắt, tránh lazy trên thẻ listing. */
    @Query("SELECT DISTINCT ci.carDefinition.id FROM Favorite f JOIN f.carInventory ci WHERE f.user = :user")
    Set<Long> findCarDefinitionIdsByUser(@Param("user") User user);

    /** Storefront + open-in-view tắt: nạp xe và ảnh để render danh sách yêu thích. */
    @Query(
            """
                    SELECT DISTINCT f FROM Favorite f
                    JOIN FETCH f.carInventory ci
                    JOIN FETCH ci.carDefinition d
                    JOIN FETCH d.brand
                    JOIN FETCH d.model
                    LEFT JOIN FETCH d.segment
                    LEFT JOIN FETCH d.images
                    WHERE f.user = :user
                    ORDER BY f.createdAt DESC
                    """)
    List<Favorite> findByUserWithDetails(@Param("user") User user);

    List<Favorite> findByCarInventory(CarInventory carInventory);

    Optional<Favorite> findByUserAndCarInventory(User user, CarInventory carInventory);

    @Query("SELECT f FROM Favorite f JOIN f.carInventory ci WHERE f.user = :user AND ci.carDefinition.id = :defId")
    List<Favorite> findByUserAndCarDefinitionId(@Param("user") User user, @Param("defId") Long defId);
}

