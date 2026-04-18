package com.fcar.modules.catalog.controller;

import com.fcar.modules.review.dto.ReviewForm;
import com.fcar.modules.catalog.entity.CarDefinition;
import com.fcar.modules.review.entity.CarReview;
import com.fcar.modules.user.entity.User;
import com.fcar.modules.order.entity.enums.OrderStatus;
import com.fcar.modules.order.repository.CarOrderRepository;
import com.fcar.modules.review.repository.CarReviewRepository;
import com.fcar.modules.favorite.repository.FavoriteRepository;
import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.catalog.service.CarQueryService;
import com.fcar.modules.catalog.service.storefront.StorefrontListingFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Phân công: Hiệp Hiếu — danh sách xe, chi tiết xe, hiển thị đánh giá. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/cars")
public class CarController {

    private final CarQueryService carQueryService;
    private final StorefrontCarListingModelHelper storefrontCarListingModelHelper;
    private final FavoriteRepository favoriteRepository;
    private final CarReviewRepository carReviewRepository;
    private final CarOrderRepository carOrderRepository;

    @GetMapping
    public String listCars(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                           @RequestParam(value = "size", required = false, defaultValue = "12") int size,
                           @RequestParam(value = "q", required = false, defaultValue = "") String q,
                           @RequestParam(value = "brands", required = false) String brands,
                           @RequestParam(value = "models", required = false) String models,
                           @RequestParam(value = "segments", required = false) String segments,
                           @RequestParam(value = "inStock", required = false) Boolean inStock,
                           @AuthenticationPrincipal FcarUserDetails principal,
                           Model model) {
        StorefrontListingFilter filter =
                StorefrontListingFilter.fromHttpParams(q, brands, models, segments, inStock);
        storefrontCarListingModelHelper.populateCarListing(
                model, page, size, principal, true, false, "Danh sách xe", filter);
        return "cars/list";
    }

    @GetMapping("/{id:\\d+}")
    public String carDetail(@PathVariable Long id, @AuthenticationPrincipal FcarUserDetails principal, Model model) {
        CarDefinition definition = carQueryService.findListedDefinitionForDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Xe không tồn tại hoặc đang tạm ngưng"));
        long totalStock = carQueryService.totalStockForDefinition(id);

        boolean favorite = false;
        ReviewForm reviewForm = new ReviewForm();
        boolean canReview = false;
        if (principal != null) {
            User user = principal.getUser();
            favorite = !favoriteRepository.findByUserAndCarDefinitionId(user, id).isEmpty();
            canReview = !carOrderRepository
                    .findByUserAndCarInventoryCarDefinitionIdAndStatus(user, id, OrderStatus.DELIVERED)
                    .isEmpty();
        }
        List<CarReview> reviews = carReviewRepository.findVisibleByCarDefinitionId(id);
        Object[] agg = carReviewRepository.getAggregateStatsByCarDefinitionId(id);
        long reviewCount = 0L;
        Double reviewAverage = null;
        if (agg != null && agg.length >= 2) {
            if (agg[0] != null) {
                reviewCount = ((Number) agg[0]).longValue();
            }
            if (agg[1] != null) {
                reviewAverage = ((Number) agg[1]).doubleValue();
            }
        }

        model.addAttribute("definition", definition);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("favorite", favorite);
        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("reviewAverage", reviewAverage);
        model.addAttribute("canReview", canReview);
        model.addAttribute("reviewForm", reviewForm);
        return "cars/detail";
    }
}
