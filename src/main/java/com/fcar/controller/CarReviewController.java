package com.fcar.controller;

import com.fcar.domain.CarInventory;
import com.fcar.domain.CarOrder;
import com.fcar.domain.CarReview;
import com.fcar.domain.User;
import com.fcar.domain.enums.OrderStatus;
import com.fcar.repository.CarOrderRepository;
import com.fcar.repository.CarReviewRepository;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.CarQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Hiệp Hiếu — gửi đánh giá xe (khách đã nhận xe). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/cars")
public class CarReviewController {

    private final CarQueryService carQueryService;
    private final CarOrderRepository carOrderRepository;
    private final CarReviewRepository carReviewRepository;

    @PostMapping("/{definitionId:\\d+}/reviews")
    public String submitReview(@PathVariable Long definitionId,
                               @Valid @ModelAttribute("reviewForm") ReviewForm form,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal FcarUserDetails principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/auth/login?redirect=/cars/" + definitionId;
        }
        User user = principal.getUser();
        if (carQueryService.findListedDefinitionForDetail(definitionId).isEmpty()) {
            return "redirect:/cars";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Đánh giá không hợp lệ: chọn số sao từ 1 đến 5.");
            return "redirect:/cars/" + definitionId;
        }

        CarOrder order = carOrderRepository
                .findByUserAndCarInventoryCarDefinitionIdAndStatus(user, definitionId, OrderStatus.DELIVERED)
                .stream()
                .findFirst()
                .orElse(null);
        if (order == null) {
            return "redirect:/cars/" + definitionId;
        }

        CarInventory car = order.getCarInventory();

        CarReview review = carReviewRepository
                .findByUserAndCarInventoryAndOrder(user, car, order)
                .orElseGet(() -> {
                    CarReview r = new CarReview();
                    r.setUser(user);
                    r.setCarInventory(car);
                    r.setOrder(order);
                    return r;
                });
        review.setRating(form.getRating());
        review.setComment(normalizeComment(form.getComment()));
        review.setHidden(true);
        carReviewRepository.save(review);

        redirectAttributes.addFlashAttribute("flashSuccess",
                "Cảm ơn bạn đã gửi đánh giá. Nội dung sẽ hiển thị sau khi được duyệt.");
        return "redirect:/cars/" + definitionId;
    }

    private static String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String t = comment.trim();
        return t.isEmpty() ? null : t;
    }

    @Data
    public static class ReviewForm {
        @NotNull
        @Min(1)
        @Max(5)
        private Integer rating;

        private String comment;
    }
}
