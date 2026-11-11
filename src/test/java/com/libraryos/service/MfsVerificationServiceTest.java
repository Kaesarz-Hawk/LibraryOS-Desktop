package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class MfsVerificationServiceTest {

    private PaymentRepository paymentRepository;
    private MfsVerificationService mfsVerificationService;

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(PaymentRepository.class);
        mfsVerificationService = new MfsVerificationService(paymentRepository);
    }

    @Test
    void testValidBkashTrxId() {
        when(paymentRepository.findByExternalReference("BLA79X021Y")).thenReturn(Optional.empty());

        String verified = mfsVerificationService.verifyAndSanitizeTrxId(PaymentMethod.BKASH, "  bla79x021y ");
        assertEquals("BLA79X021Y", verified);
    }

    @Test
    void testInvalidBkashTrxIdThrowsException() {
        // Less than 10 characters
        assertThrows(ValidationException.class, () ->
                mfsVerificationService.verifyAndSanitizeTrxId(PaymentMethod.BKASH, "SHORT12"));

        // Non-alphanumeric
        assertThrows(ValidationException.class, () ->
                mfsVerificationService.verifyAndSanitizeTrxId(PaymentMethod.BKASH, "BLA-79X-02"));
    }

    @Test
    void testValidNagadAndRocketTrxId() {
        when(paymentRepository.findByExternalReference("NGD88412")).thenReturn(Optional.empty());
        when(paymentRepository.findByExternalReference("202611048821")).thenReturn(Optional.empty());

        String nagad = mfsVerificationService.verifyAndSanitizeTrxId(PaymentMethod.NAGAD, "ngd88412");
        assertEquals("NGD88412", nagad);

        String rocket = mfsVerificationService.verifyAndSanitizeTrxId(PaymentMethod.ROCKET, "202611048821");
        assertEquals("202611048821", rocket);
    }

    @Test
    void testDuplicateTrxIdThrowsException() {
        Payment existingPayment = new Payment();
        existingPayment.setReceiptNumber("REC-20261104-0001");
        existingPayment.setExternalReference("DUPTRX1234");

        when(paymentRepository.findByExternalReference("DUPTRX1234")).thenReturn(Optional.of(existingPayment));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                mfsVerificationService.verifyAndSanitizeTrxId(PaymentMethod.BKASH, "DUPTRX1234"));
        assertTrue(ex.getMessage().contains("Duplicate payments are prohibited"));
    }
}

