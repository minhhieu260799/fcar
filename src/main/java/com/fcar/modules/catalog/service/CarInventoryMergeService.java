package com.fcar.modules.catalog.service;

import com.fcar.modules.catalog.entity.CarInventory;
import com.fcar.modules.favorite.entity.Favorite;
import com.fcar.modules.catalog.repository.CarInventoryRepository;
import com.fcar.modules.order.repository.CarOrderRepository;
import com.fcar.modules.review.repository.CarReviewRepository;
import com.fcar.modules.contact.repository.ContactRequestRepository;
import com.fcar.modules.favorite.repository.FavoriteRepository;
import com.fcar.modules.testdrive.repository.TestDriveBookingRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Phân công: Hiếu — gộp/xóa bản ghi kho khi import (admin). */
@Service
@RequiredArgsConstructor
public class CarInventoryMergeService {

    private final CarInventoryRepository carInventoryRepository;
    private final CarOrderRepository carOrderRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final TestDriveBookingRepository testDriveBookingRepository;
    private final CarReviewRepository carReviewRepository;
    private final FavoriteRepository favoriteRepository;

    /**
     * Gộp {@code source} vào {@code target}: chuyển mọi tham chiếu sang {@code target},
     * cộng số lượng vào {@code target}, xóa {@code source} (giữ id của {@code target}).
     */
    @Transactional
    public void mergeInto(CarInventory source, CarInventory target) {
        if (source.getId().equals(target.getId())) {
            return;
        }
        carOrderRepository.reassignCarInventory(source, target);
        contactRequestRepository.reassignCarInventory(source, target);
        testDriveBookingRepository.reassignCarInventory(source, target);
        carReviewRepository.reassignCarInventory(source, target);

        List<Favorite> favorites = favoriteRepository.findByCarInventory(source);
        for (Favorite f : favorites) {
            Optional<Favorite> existing = favoriteRepository.findByUserAndCarInventory(f.getUser(), target);
            if (existing.isPresent()) {
                favoriteRepository.delete(f);
            } else {
                f.setCarInventory(target);
                favoriteRepository.save(f);
            }
        }

        target.setQuantity(target.getQuantity() + source.getQuantity());
        carInventoryRepository.save(target);
        carInventoryRepository.delete(source);
    }
}
