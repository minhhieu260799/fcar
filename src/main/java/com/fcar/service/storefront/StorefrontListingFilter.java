package com.fcar.service.storefront;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Phân công: Hiệp Hiếu — bộ lọc/lọc từ query string danh sách xe storefront.
 */
public record StorefrontListingFilter(
        String q,
        List<Long> brandIds,
        List<Long> modelIds,
        List<Long> segmentIds,
        boolean inStockOnly) {

    public StorefrontListingFilter {
        q = q == null ? "" : q.trim();
        brandIds = normalizeIdList(brandIds);
        modelIds = normalizeIdList(modelIds);
        segmentIds = normalizeIdList(segmentIds);
    }

    private static List<Long> normalizeIdList(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (Long id : raw) {
            if (id != null && id > 0) {
                set.add(id);
            }
        }
        return List.copyOf(set);
    }

    /** Chuỗi {@code 1,2,3} từ query (GET) hoặc rỗng; bỏ qua token không phải số. */
    public static List<Long> parseIdList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String part : csv.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // bỏ qua
            }
        }
        return out;
    }

    public static StorefrontListingFilter fromHttpParams(
            String q, String brandsCsv, String modelsCsv, String segmentsCsv, Boolean inStock) {
        return new StorefrontListingFilter(
                q == null ? "" : q,
                parseIdList(brandsCsv),
                parseIdList(modelsCsv),
                parseIdList(segmentsCsv),
                Boolean.TRUE.equals(inStock));
    }

    public static StorefrontListingFilter empty() {
        return new StorefrontListingFilter("", List.of(), List.of(), List.of(), false);
    }

    public String qOrNull() {
        return q.isEmpty() ? null : q;
    }

    public boolean hasEffectiveFilters() {
        return !q.isEmpty()
                || !brandIds.isEmpty()
                || !modelIds.isEmpty()
                || !segmentIds.isEmpty()
                || inStockOnly;
    }

    public String brandsParam() {
        return joinIds(brandIds);
    }

    public String modelsParam() {
        return joinIds(modelIds);
    }

    public String segmentsParam() {
        return joinIds(segmentIds);
    }

    private static String joinIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public String toDescriptionSnippet() {
        StringBuilder sb = new StringBuilder();
        if (!q.isEmpty()) {
            sb.append("q=").append(q.toLowerCase(Locale.ROOT));
        }
        if (!brandIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("brands=").append(brandsParam());
        }
        if (!modelIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("models=").append(modelsParam());
        }
        if (!segmentIds.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("segments=").append(segmentsParam());
        }
        if (inStockOnly) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("inStock=true");
        }
        return sb.toString();
    }
}
