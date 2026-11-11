package com.libraryos.service;

import com.libraryos.core.util.CurrencyFormatUtil;
import com.libraryos.model.member.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Enterprise Notification Gateway dispatching localized SMS and email notices
 * for overdue reminders, hold pickups, and fine payment acknowledgments.
 */
public class NotificationGatewayService {

    private static final Logger log = LoggerFactory.getLogger(NotificationGatewayService.class);

    // Bangladeshi phone format: 013-019 followed by 8 digits
    private static final Pattern BD_PHONE_PATTERN = Pattern.compile("^(?:\\+?88)?01[3-9]\\d{8}$");

    private final NotificationService notificationService;

    public NotificationGatewayService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Normalizes a Bangladeshi mobile number into standard E.164 format (+8801XXXXXXXXX).
     */
    public String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String clean = phone.replaceAll("[^0-9+]", "");
        if (clean.startsWith("+880")) {
            return clean;
        } else if (clean.startsWith("880")) {
            return "+" + clean;
        } else if (clean.startsWith("01")) {
            return "+88" + clean;
        }
        return clean;
    }

    public boolean isValidBdPhoneNumber(String phone) {
        if (phone == null) return false;
        String clean = phone.replaceAll("[^0-9+]", "");
        return BD_PHONE_PATTERN.matcher(clean).matches();
    }

    /**
     * Dispatches an overdue loan warning SMS and in-app alert.
     */
    public boolean sendOverdueAlert(Member member, String bookTitle, int overdueDays, BigDecimal fineBdt) {
        if (member == null) return false;

        String phone = normalizePhoneNumber(member.getPhone());
        String msgEn = String.format("Library Alert: '%s' is overdue by %d day(s). Accumulated fine: %s. Please return promptly.",
                bookTitle, overdueDays, CurrencyFormatUtil.format(fineBdt));
        String msgBn = String.format("লাইব্রেরি নোটিশ: '%s' বইটি %d দিন বিলম্বিত। মোট জরিমানা: %s। অনুগ্রহ করে দ্রুত জমা দিন।",
                bookTitle, overdueDays, CurrencyFormatUtil.format(fineBdt));

        log.info("[SMS GATEWAY -> {}] {}", phone, msgEn);

        // Record in-app notification if member user exists
        if (notificationService != null && member.getId() != null) {
            notificationService.sendNotification(member.getId(), "Overdue Book Notice / দেরিতে বই জমা", msgBn, "OVERDUE");
        }
        return true;
    }

    /**
     * Dispatches a hold reservation ready-for-pickup SMS.
     */
    public boolean sendHoldReadyAlert(Member member, String bookTitle, LocalDate expiryDate) {
        if (member == null) return false;

        String phone = normalizePhoneNumber(member.getPhone());
        String msg = String.format("Library Alert: Your reserved book '%s' is ready at Circulation Desk. Please collect by %s.",
                bookTitle, expiryDate.toString());

        log.info("[SMS GATEWAY -> {}] {}", phone, msg);

        if (notificationService != null && member.getId() != null) {
            notificationService.sendNotification(member.getId(), "Reserved Book Ready / সংরক্ষিত বই প্রস্তুত",
                    "বই: " + bookTitle + "। সংগ্রহের শেষ তারিখ: " + expiryDate, "HOLD_PICKUP");
        }
        return true;
    }

    /**
     * Dispatches payment receipt confirmation SMS.
     */
    public boolean sendPaymentReceiptAlert(Member member, String receiptNumber, BigDecimal amountBdt) {
        if (member == null) return false;

        String phone = normalizePhoneNumber(member.getPhone());
        String msg = String.format("LibraryOS Payment: Received %s under Receipt #%s for %s. Thank you.",
                CurrencyFormatUtil.format(amountBdt), receiptNumber, member.getFullNameEn());

        log.info("[SMS GATEWAY -> {}] {}", phone, msg);

        if (notificationService != null && member.getId() != null) {
            notificationService.sendNotification(member.getId(), "Payment Receipt / ফি পরিশোধ রসিদ",
                    "রসিদ নম্বর: " + receiptNumber + ", পরিমাণ: " + CurrencyFormatUtil.format(amountBdt), "PAYMENT");
        }
        return true;
    }
}
