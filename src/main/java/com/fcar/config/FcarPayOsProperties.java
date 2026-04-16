package com.fcar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Phân công: Minh — cấu hình ứng dụng PayOS (application.yml). */
@Data
@ConfigurationProperties(prefix = "fcar.payos")
public class FcarPayOsProperties {

    /**
     * Bật khi đã có kênh PayOS và điền đủ client-id, api-key, checksum-key trên my.payos.vn.
     */
    private boolean enabled;

    private String clientId = "";

    private String apiKey = "";

    /** Checksum key của kênh thanh toán (dùng ký request tạo link + verify webhook). */
    private String checksumKey = "";

    private String baseUrl = "https://api-merchant.payos.vn";

    /** URL gốc site (không có / cuối), dùng cho returnUrl/cancelUrl PayOS. */
    private String publicBaseUrl = "http://localhost:8080";
}
