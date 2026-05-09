package com.fcar.modules.catalog.controller;

import com.fcar.modules.user.security.FcarUserDetails;
import com.fcar.modules.catalog.service.storefront.StorefrontListingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Phân công: Hiệp Hiếu — trang chủ, danh sách xe niêm yết. */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StorefrontCarListingModelHelper storefrontCarListingModelHelper;

    @GetMapping("/")
    public String home(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
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
                model, page, size, principal, false, true, "Trang chủ", filter);
        return "index";
    }
}
