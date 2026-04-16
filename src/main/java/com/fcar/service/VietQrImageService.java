package com.fcar.service;

import com.fcar.domain.StoreBankAccount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Phân công: Minh — ảnh QR VietQR cho chuyển khoản đặt cọc.
 * Tạo URL qua {@code img.vietqr.io}; tài liệu: <a href="https://vietqr.io/">vietqr.io</a>.
 */
@Service
public class VietQrImageService {

    private static final String IMAGE_PREFIX = "https://img.vietqr.io/image/";

    @Value("${fcar.vietqr.template:compact2}")
    private String template;

    /**
     * @return URL ảnh PNG/JPG nếu đủ BIN + số TK; rỗng nếu thiếu cấu hình
     */
    public Optional<String> buildImageUrl(StoreBankAccount account, BigDecimal amountVnd, String addInfo) {
        if (account == null || account.getBankBin() == null || account.getBankBin().isBlank()) {
            return Optional.empty();
        }
        String bin = account.getBankBin().trim().replaceAll("\\s+", "");
        if (!bin.matches("\\d{6}")) {
            return Optional.empty();
        }
        String accountNo = account.getAccountNumber() == null
                ? ""
                : account.getAccountNumber().trim().replaceAll("\\s+", "");
        if (accountNo.isEmpty()) {
            return Optional.empty();
        }
        long amount;
        try {
            amount = amountVnd.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            amount = amountVnd.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        if (amount <= 0) {
            return Optional.empty();
        }

        String fileBase = bin + "-" + accountNo + "-" + template + ".jpg";
        String add = sanitizeVietQrText(addInfo, 50);
        String name = sanitizeVietQrText(account.getAccountName(), 50);

        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(IMAGE_PREFIX + fileBase)
                .queryParam("amount", amount)
                .queryParam("addInfo", add);
        if (!name.isEmpty()) {
            b.queryParam("accountName", name);
        }
        return Optional.of(b.encode().build().toUriString());
    }

    /**
     * VietQR khuyến nghị nội dung không dấu, giới hạn độ dài.
     */
    public static String sanitizeVietQrText(String input, int maxLen) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        StringBuilder sb = new StringBuilder();
        for (char c : normalized.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '.' || c == '-') {
                sb.append(c);
            }
        }
        String s = sb.toString().trim().replaceAll("\\s+", " ");
        if (s.length() > maxLen) {
            return s.substring(0, maxLen);
        }
        return s;
    }
}
