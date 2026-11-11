package com.libraryos.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility for formatting currency in Bangladesh Taka (BDT ৳).
 */
public final class CurrencyFormatUtil {

    public static final String CURRENCY_SYMBOL = "৳";
    private static final DecimalFormat BDT_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        BDT_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private CurrencyFormatUtil() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return CURRENCY_SYMBOL + " 0.00";
        }
        return CURRENCY_SYMBOL + " " + BDT_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    public static BigDecimal parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String clean = rawText.replace(CURRENCY_SYMBOL, "").replace(",", "").trim();
        return new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
    }
}
