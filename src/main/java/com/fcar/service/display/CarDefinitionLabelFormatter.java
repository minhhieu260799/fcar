package com.fcar.service.display;

import com.fcar.domain.CarDefinition;
import org.springframework.stereotype.Component;

/**
 * Phân công: dùng chung — nhãn hiển thị mẫu xe (Hiệp Hiếu storefront; Hiếu admin; Minh/Bảo email).
 * Chuẩn: {@code Thương hiệu Dòng phân khúc - Năm}.
 */
@Component
public class CarDefinitionLabelFormatter {

    /**
     * Tiêu đề trên thẻ / danh sách: thương hiệu + dòng xe + phân khúc (không năm).
     * Ví dụ: {@code Toyota Camry 2.0Q}, {@code Honda Civic G}.
     */
    public String formatListingTitle(CarDefinition def) {
        if (def == null) {
            return "";
        }
        String brand = safe(def.getBrand() != null ? def.getBrand().getName() : null);
        String model = safe(def.getModel() != null ? def.getModel().getName() : null);
        String segment = "";
        if (def.getSegment() != null && def.getSegment().getName() != null) {
            segment = safe(def.getSegment().getName());
        }

        StringBuilder sb = new StringBuilder();
        if (!brand.isEmpty()) {
            sb.append(brand);
        }
        if (!model.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(model);
        }
        if (!segment.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(segment);
        }
        return sb.toString();
    }

    /** Chuỗi đầy đủ: {@link #formatListingTitle} + {@code  - năm}. */
    public String format(CarDefinition def) {
        if (def == null) {
            return "";
        }
        String base = formatListingTitle(def);
        Integer year = def.getProductionYear();
        if (year != null) {
            if (base.isEmpty()) {
                return String.valueOf(year);
            }
            return base + " - " + year;
        }
        return base;
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.isEmpty() ? "" : t;
    }
}
