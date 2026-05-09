package com.fcar.modules.favorite.service;

import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.catalog.service.CarQueryService;
import com.fcar.modules.favorite.entity.Favorite;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.favorite.repository.FavoriteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phân công: Hiệp Hiếu — nghiệp vụ yêu thích. */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final CarQueryService carQueryService;

    /**
     * Bật/tắt yêu thích theo mẫu xe (definition).
     *
     * @return true nếu sau khi gọi xe đang trong danh sách yêu thích
     */
    @Transactional
    public boolean toggleFavorite(User user, Long definitionId) {
        List<Favorite> existing = favoriteRepository.findByUserAndCarDefinitionId(user, definitionId);
        if (!existing.isEmpty()) {
            favoriteRepository.deleteAll(existing);
            return false;
        }
        CarInventory inv = carQueryService
                .resolveRepresentativeInventory(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Không có dòng kho cho mẫu xe này"));
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setCarInventory(inv);
        favoriteRepository.save(favorite);
        return true;
    }
}
