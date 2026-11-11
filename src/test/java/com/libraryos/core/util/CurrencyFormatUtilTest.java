package com.libraryos.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyFormatUtilTest {

    @Test
    @DisplayName("Should format BDT currency with ৳ symbol and 2 decimal places")
    void testFormatBdt() {
        assertEquals("৳ 150.00", CurrencyFormatUtil.format(new BigDecimal("150")));
        assertEquals("৳ 1,250.50", CurrencyFormatUtil.format(new BigDecimal("1250.50")));
        assertEquals("৳ 0.00", CurrencyFormatUtil.format((BigDecimal) null));
        assertEquals("৳ 5.25", CurrencyFormatUtil.format(5.25));
    }

    @Test
    @DisplayName("Should parse BDT strings to BigDecimal")
    void testParseBdt() {
        assertEquals(new BigDecimal("150.00"), CurrencyFormatUtil.parse("৳ 150.00"));
        assertEquals(new BigDecimal("1250.50"), CurrencyFormatUtil.parse("1,250.50"));
        assertEquals(new BigDecimal("0.00"), CurrencyFormatUtil.parse(""));
    }
}

