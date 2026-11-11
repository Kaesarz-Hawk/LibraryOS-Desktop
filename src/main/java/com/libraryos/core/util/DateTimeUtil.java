package com.libraryos.core.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and time formatting and Bangladesh holiday/weekend utilities.
 */
public final class DateTimeUtil {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateTimeUtil() {
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return "—";
        return date.format(DATE_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static String formatMediumDateTime(LocalDateTime dateTime) {
        return formatDateTime(dateTime);
    }

    /**
     * Checks if the date falls on Friday (traditional weekly holiday in Bangladesh).
     */
    public static boolean isBangladeshWeeklyHoliday(LocalDate date) {
        if (date == null) return false;
        return date.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    /**
     * Advances date to next working day if it lands on a weekly holiday.
     */
    public static LocalDate adjustForWeeklyHoliday(LocalDate date) {
        if (date == null) return null;
        LocalDate current = date;
        while (isBangladeshWeeklyHoliday(current)) {
            current = current.plusDays(1);
        }
        return current;
    }
}
