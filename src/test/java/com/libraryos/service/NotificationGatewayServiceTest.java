package com.libraryos.service;

import com.libraryos.model.member.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NotificationGatewayServiceTest {

    private NotificationService notificationService;
    private NotificationGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        gatewayService = new NotificationGatewayService(notificationService);
    }

    @Test
    void testPhoneNumberNormalization() {
        assertEquals("+8801711223344", gatewayService.normalizePhoneNumber("01711223344"));
        assertEquals("+8801811223344", gatewayService.normalizePhoneNumber("8801811223344"));
        assertEquals("+8801911223344", gatewayService.normalizePhoneNumber("+8801911223344"));
        assertEquals("+8801711223344", gatewayService.normalizePhoneNumber("01711-223344"));
    }

    @Test
    void testPhoneNumberValidation() {
        assertTrue(gatewayService.isValidBdPhoneNumber("01711223344"));
        assertTrue(gatewayService.isValidBdPhoneNumber("+8801911223344"));
        assertTrue(gatewayService.isValidBdPhoneNumber("8801311223344"));

        assertFalse(gatewayService.isValidBdPhoneNumber("01211223344")); // invalid operator 012
        assertFalse(gatewayService.isValidBdPhoneNumber("1234567")); // too short
        assertFalse(gatewayService.isValidBdPhoneNumber("abcdefghijk")); // letters
    }

    @Test
    void testDispatchOverdueAlert() {
        Member m = new Member();
        m.setId(10L);
        m.setFullNameEn("Tanvir Ahmed");
        m.setPhone("+8801711223344");

        boolean sent = gatewayService.sendOverdueAlert(m, "Clean Code", 5, new BigDecimal("25.00"));
        assertTrue(sent);
    }

    @Test
    void testDispatchHoldReadyAndPaymentAlert() {
        Member m = new Member();
        m.setId(20L);
        m.setFullNameEn("Sadia Khan");
        m.setPhone("01812345678");

        boolean holdSent = gatewayService.sendHoldReadyAlert(m, "Gitanjali", LocalDate.now().plusDays(3));
        assertTrue(holdSent);

        boolean paySent = gatewayService.sendPaymentReceiptAlert(m, "REC-20261104-0001", new BigDecimal("100.00"));
        assertTrue(paySent);
    }
}

