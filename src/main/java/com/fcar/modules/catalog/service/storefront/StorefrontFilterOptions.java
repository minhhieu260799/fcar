package com.fcar.modules.catalog.service.storefront;

import java.util.List;

/** Phân công: Hiệp Hiếu — tùy chọn bộ lọc (brand/model/segment) trên storefront. */
public record StorefrontFilterOptions(
        List<StorefrontBrandOption> brands,
        List<StorefrontModelOption> models,
        List<StorefrontSegmentOption> segments) {

    public record StorefrontBrandOption(Long id, String name) {}

    public record StorefrontModelOption(Long id, String name, Long brandId) {}

    public record StorefrontSegmentOption(Long id, String name, Long modelId) {}
}
