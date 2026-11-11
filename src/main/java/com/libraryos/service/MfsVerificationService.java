package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates and verifies external Mobile Financial Services (MFS) transaction IDs for bKash, Nagad, and Rocket.
 */
public class MfsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(MfsVerificationService.class);

    // Standard Bangladesh MFS TrxID regex patterns
    private static final Pattern BKASH_PATTERN = Pattern.compile("^[A-Z0-9]{10}$");
    private static final Pattern NAGAD_PATTERN = Pattern.compile("^[A-Z0-9]{8}$");
    private static final Pattern ROCKET_PATTERN = Pattern.compile("^[0-9]{12}$");

    private final PaymentRepository paymentRepository;

    public MfsVerificationService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Validates syntax and verifies uniqueness of an MFS Transaction ID.
     */
    public String verifyAndSanitizeTrxId(PaymentMethod method, String rawTrxId) {
        if (method == null || method == PaymentMethod.CASH) {
            return null; // Not applicable for cash
        }

        if (rawTrxId == null || rawTrxId.trim().isBlank()) {
            throw new ValidationException("Transaction ID (TrxID) is required for " + method.name() + " payments.");
        }

        String sanitized = rawTrxId.trim().toUpperCase();

        // Syntax format verification
        switch (method) {
            case BKASH -> {
                if (!BKASH_PATTERN.matcher(sanitized).matches()) {
                    throw new ValidationException("Invalid bKash Transaction ID format. Must be 10 alphanumeric characters (e.g., BLA79X021Y).");
                }
            }
            case NAGAD -> {
                if (!NAGAD_PATTERN.matcher(sanitized).matches()) {
                    throw new ValidationException("Invalid Nagad Transaction ID format. Must be 8 alphanumeric characters (e.g., NGD88412).");
                }
            }
            case ROCKET -> {
                if (!ROCKET_PATTERN.matcher(sanitized).matches()) {
                    throw new ValidationException("Invalid Rocket Transaction ID format. Must be 12 digits (e.g., 202611048821).");
                }
            }
            default -> {
                // Bank transfer / other: require at least 4 characters
                if (sanitized.length() < 4) {
                    throw new ValidationException("Transaction reference must contain at least 4 characters.");
                }
            }
        }

        // Duplicate TrxID verification in database
        if (paymentRepository != null) {
            Optional<Payment> existing = paymentRepository.findByExternalReference(sanitized);
            if (existing.isPresent()) {
                throw new ValidationException("Transaction ID '" + sanitized + "' has already been recorded in Receipt #" +
                        existing.get().getReceiptNumber() + ". Duplicate payments are prohibited.");
            }
        }

        log.debug("MFS TrxID verified: {} for {}", sanitized, method);
        return sanitized;
    }
}

