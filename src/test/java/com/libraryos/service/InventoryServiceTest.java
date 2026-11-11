package com.libraryos.service;

import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.repository.BookCopyRepository;
import com.libraryos.repository.ShelfRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private BookCopyRepository copyRepository;
    @Mock
    private ShelfRepository shelfRepository;
    @Mock
    private AuditService auditService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(copyRepository, shelfRepository, auditService);
        UserSession session = new UserSession(1L, "librarian", "Librarian", null, null,
                Collections.singleton("LIBRARIAN"),
                Set.of("COPY_MANAGE", "BOOK_VIEW"));
        SessionManager.setSession(session);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should add physical copy when valid and unique")
    void testAddCopySuccess() {
        BookCopy copy = new BookCopy();
        copy.setBookId(1L);
        copy.setAccessionNumber("ACC-2026-9999");
        copy.setBarcode("BC999999");
        copy.setPurchaseCost(new BigDecimal("500.00"));

        when(copyRepository.findByAccessionNumber("ACC-2026-9999")).thenReturn(Optional.empty());
        when(copyRepository.findByBarcode("BC999999")).thenReturn(Optional.empty());

        BookCopy saved = new BookCopy();
        saved.setId(50L);
        saved.setAccessionNumber("ACC-2026-9999");
        saved.setBarcode("BC999999");
        when(copyRepository.create(any(BookCopy.class))).thenReturn(saved);

        BookCopy result = inventoryService.addCopy(copy);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        verify(auditService).log(eq("COPY_ADDED"), eq("BOOK_COPY"), eq("50"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject duplicate accession number")
    void testDuplicateAccession() {
        BookCopy copy = new BookCopy();
        copy.setBookId(1L);
        copy.setAccessionNumber("ACC-2026-0001");
        copy.setBarcode("BC123456");

        when(copyRepository.findByAccessionNumber("ACC-2026-0001")).thenReturn(Optional.of(new BookCopy()));

        ValidationException ex = assertThrows(ValidationException.class, () -> inventoryService.addCopy(copy));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("Should reject duplicate barcode")
    void testDuplicateBarcode() {
        BookCopy copy = new BookCopy();
        copy.setBookId(1L);
        copy.setAccessionNumber("ACC-2026-0002");
        copy.setBarcode("BC100001");

        when(copyRepository.findByAccessionNumber("ACC-2026-0002")).thenReturn(Optional.empty());
        when(copyRepository.findByBarcode("BC100001")).thenReturn(Optional.of(new BookCopy()));

        ValidationException ex = assertThrows(ValidationException.class, () -> inventoryService.addCopy(copy));
        assertTrue(ex.getMessage().contains("Barcode 'BC100001' is already registered"));
    }

    @Test
    @DisplayName("Should record status change with reason")
    void testChangeStatus() {
        inventoryService.changeCopyStatus(5L, CopyStatus.UNDER_MAINTENANCE, "Pages torn, binding repair needed");

        verify(copyRepository).updateStatus(eq(5L), eq(CopyStatus.UNDER_MAINTENANCE), eq(1L), eq("Pages torn, binding repair needed"));
        verify(auditService).log(eq("COPY_STATUS_CHANGED"), eq("BOOK_COPY"), eq("5"), anyString(), eq("Pages torn, binding repair needed"));
    }
}
