package com.fcar.controller;

import com.fcar.repository.FavoriteRepository;
import com.fcar.security.FcarUserDetails;
import com.fcar.service.CarQueryService;
import com.fcar.service.CarQueryService.ListedCarDefinition;
import com.fcar.service.storefront.StorefrontListingFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/**
 * Phân công: Hiệp Hiếu — model dùng chung trang chủ & danh sách xe (lọc, phân trang).
 */
@Component
@RequiredArgsConstructor
public class StorefrontCarListingModelHelper {

    private final CarQueryService carQueryService;
    private final FavoriteRepository favoriteRepository;

    public void populateCarListing(
            Model model,
            int page,
            int size,
            FcarUserDetails principal,
            boolean showCarListPageTitle,
            boolean carListOnHome,
            String pageTitle,
            StorefrontListingFilter listingFilter) {
        StorefrontListingFilter f = listingFilter == null ? StorefrontListingFilter.empty() : listingFilter;
        Page<ListedCarDefinition> result = carQueryService.listDefinitionsForStorefront(page, size, f);

        Set<Long> favoriteDefinitionIds =
                principal != null ? favoriteRepository.findCarDefinitionIdsByUser(principal.getUser()) : Set.of();

        List<ListedCarDefinition> items = new ArrayList<>(result.getContent());
        if (!favoriteDefinitionIds.isEmpty()) {
            List<ListedCarDefinition> favFirst = items.stream()
                    .filter(c -> favoriteDefinitionIds.contains(c.definition().getId()))
                    .collect(Collectors.toList());
            List<ListedCarDefinition> others = items.stream()
                    .filter(c -> !favoriteDefinitionIds.contains(c.definition().getId()))
                    .collect(Collectors.toList());
            favFirst.addAll(others);
            items = favFirst;
        }

        model.addAttribute("title", pageTitle);
        model.addAttribute("page", result);
        model.addAttribute("carItems", items);
        model.addAttribute("favoriteDefinitionIds", favoriteDefinitionIds);
        model.addAttribute("showCarListPageTitle", showCarListPageTitle);
        model.addAttribute("carListOnHome", carListOnHome);
        model.addAttribute("searchQuery", f.q());
        model.addAttribute("listingFilter", f);
        model.addAttribute("filterOptions", carQueryService.loadStorefrontFilterOptions());
    }
}
