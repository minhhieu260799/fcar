package com.fcar.core.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Vietnamese-style money display for HTML emails (number + đ suffix).
 */
public final class MoneyFormat {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private MoneyFormat() {
    }

    public static String formatVnd(BigDecimal v) {
        if (v == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(VI);
        return nf.format(v) + " đ";
    }
}
