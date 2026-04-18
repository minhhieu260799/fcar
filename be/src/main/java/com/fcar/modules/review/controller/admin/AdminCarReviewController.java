package com.fcar.modules.review.controller.admin;

import com.fcar.modules.review.entity.CarReview;
import com.fcar.modules.review.repository.CarReviewRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Phân công: Hiệp Hiếu — duyệt/hiện đánh giá xe (admin). */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reviews")
public class AdminCarReviewController {

    private final CarReviewRepository carReviewRepository;

    @GetMapping
    public String list(@RequestParam(value = "filter", required = false, defaultValue = "all") String filter,
                       Model model) {
        List<CarReview> all = carReviewRepository.findAllForAdminOrderByCreatedAtDesc();
        List<CarReview> reviews = switch (normalizeFilter(filter)) {
            case "pending" -> all.stream().filter(CarReview::isHidden).collect(Collectors.toList());
            case "visible" -> all.stream().filter(r -> !r.isHidden()).collect(Collectors.toList());
            default -> all;
        };
        model.addAttribute("reviews", reviews);
        model.addAttribute("filter", normalizeFilter(filter));
        model.addAttribute("title", "Duyệt đánh giá | FCAR Admin");
        return "admin/reviews/list";
    }

    private static String normalizeFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return "all";
        }
        String f = filter.trim().toLowerCase();
        if ("pending".equals(f) || "visible".equals(f)) {
            return f;
        }
        return "all";
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id,
                          @RequestParam(value = "filter", required = false) String filter,
                          RedirectAttributes redirectAttributes) {
        CarReview review = carReviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setHidden(false);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã duyệt hiển thị đánh giá.");
        return redirectToList(filter);
    }

    @PostMapping("/{id}/hide")
    @Transactional
    public String hide(@PathVariable Long id,
                       @RequestParam(value = "filter", required = false) String filter,
                       RedirectAttributes redirectAttributes) {
        CarReview review = carReviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setHidden(true);
        redirectAttributes.addFlashAttribute("flashSuccess", "Đã ẩn đánh giá khỏi trang xe.");
        return redirectToList(filter);
    }

    private static String redirectToList(String filter) {
        String f = filter == null ? "all" : filter.trim().toLowerCase();
        if (f.isEmpty() || "all".equals(f)) {
            return "redirect:/admin/reviews";
        }
        if ("pending".equals(f) || "visible".equals(f)) {
            return "redirect:/admin/reviews?filter=" + f;
        }
        return "redirect:/admin/reviews";
    }
}
