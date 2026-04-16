package com.fcar.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class VietnamPhoneRules {

    public static final Pattern VN_PHONE = Pattern.compile("^0\\d{9}$");
    public static final Pattern OTP_SIX_DIGITS = Pattern.compile("^\\d{6}$");

    private VietnamPhoneRules() {
    }

    public static Map<String, String> validatePhoneRaw(String raw) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!StringUtils.hasText(raw)) {
            errors.put("phone", "Vui lòng nhập số điện thoại");
            return errors;
        }
        if (raw.chars().anyMatch(Character::isLetter)) {
            errors.put("phone", "Không được nhập chữ cái");
            return errors;
        }
        String normalized = normalizeDigits(raw);
        if (!VN_PHONE.matcher(normalized).matches()) {
            errors.put("phone", "Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng 0");
        }
        return errors;
    }

    public static String normalizeDigits(String raw) {
        return raw.replaceAll("\\D", "");
    }

    public static void addOtpCodeErrors(String code, Map<String, String> errors) {
        if (!StringUtils.hasText(code)) {
            errors.put("code", "Vui lòng nhập mã OTP");
        } else if (!OTP_SIX_DIGITS.matcher(code.trim()).matches()) {
            errors.put("code", "Mã OTP gồm 6 chữ số");
        }
    }
}
