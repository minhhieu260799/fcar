package com.fcar.controller;

import com.fcar.security.FcarUserDetails;
import com.fcar.service.storefront.StorefrontListingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Phân công: Hiệp Hiếu — fragment HTML danh sách xe (fetch/HTMX).
 */
@Controller
@RequiredArgsConstructor
public class StorefrontCarFragmentController {

    private final StorefrontCarListingModelHelper storefrontCarListingModelHelper;

    @GetMapping("/fragments/storefront-car-listing")
    public String storefrontCarListingFragment(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String brands,
            @RequestParam(required = false) String models,
            @RequestParam(required = false) String segments,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "true") boolean carListOnHome,
            @AuthenticationPrincipal FcarUserDetails principal,
            Model model) {
        StorefrontListingFilter filter =
                StorefrontListingFilter.fromHttpParams(q, brands, models, segments, inStock);
        storefrontCarListingModelHelper.populateCarListing(
                model,
                page,
                size,
                principal,
                !carListOnHome,
                carListOnHome,
                carListOnHome ? "Trang chủ" : "Danh sách xe",
                filter);
        return "fragments/storefront-car-list :: carListingContent";
    }
}
