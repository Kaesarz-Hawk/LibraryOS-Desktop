package com.libraryos.service;

import com.libraryos.model.catalog.Book;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.inventory.Shelf;
import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.repository.BookCopyRepository;
import com.libraryos.repository.BorrowingPolicyRepository;
import com.libraryos.repository.ShelfRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AiAssistantServiceTest {

    private CatalogService catalogService;
    private RecommendationService recommendationService;
    private BorrowingPolicyRepository borrowingPolicyRepository;
    private BookCopyRepository bookCopyRepository;
    private ShelfRepository shelfRepository;
    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        catalogService = Mockito.mock(CatalogService.class);
        recommendationService = Mockito.mock(RecommendationService.class);
        borrowingPolicyRepository = Mockito.mock(BorrowingPolicyRepository.class);
        bookCopyRepository = Mockito.mock(BookCopyRepository.class);
        shelfRepository = Mockito.mock(ShelfRepository.class);

        aiAssistantService = new AiAssistantService(
                catalogService,
                recommendationService,
                borrowingPolicyRepository,
                bookCopyRepository,
                shelfRepository
        );
    }

    @Test
    void testFineInquiryInEnglishAndBangla() {
        AiAssistantService.AiResponse respEn = aiAssistantService.processQuery("What is the overdue fine rate per day?");
        assertEquals("POLICY_FINES", respEn.intent());
        assertTrue(respEn.replyMarkdown().contains("5.00"));

        AiAssistantService.AiResponse respBn = aiAssistantService.processQuery("দেরিতে বই ফেরত দিলে জরিমানা কত?");
        assertEquals("POLICY_FINES", respBn.intent());
        assertTrue(respBn.replyMarkdown().contains("৫ টাকা"));
    }

    @Test
    void testBorrowingPolicyInquiry() {
        BorrowingPolicy studentPolicy = new BorrowingPolicy();
        studentPolicy.setMemberTypeName("STUDENT");
        studentPolicy.setMaxActiveLoans(3);
        studentPolicy.setLoanDurationDays(14);
        studentPolicy.setMaxRenewals(1);
        studentPolicy.setFinePerDayBdt(new BigDecimal("5.00"));

        when(borrowingPolicyRepository.findAll()).thenReturn(List.of(studentPolicy));

        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("How many books can a student borrow?");
        assertEquals("POLICY_BORROWING", resp.intent());
        assertTrue(resp.replyMarkdown().contains("3 books"));
    }

    @Test
    void testReservationRulesInquiry() {
        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("What is the procedure for book reservation?");
        assertEquals("POLICY_RESERVATIONS", resp.intent());
        assertTrue(resp.replyMarkdown().contains("FIFO"));
    }

    @Test
    void testCatalogSearchViaAi() {
        Book b = new Book();
        b.setId(1L);
        b.setTitleEn("Design Patterns");
        b.setAvailableCopies(2);

        when(catalogService.searchBooks(anyString())).thenReturn(List.of(b));

        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("Find books about Design Patterns");
        assertEquals("CATALOG_SEARCH", resp.intent());
        assertFalse(resp.relatedBooks().isEmpty());
        assertEquals("Design Patterns", resp.relatedBooks().get(0).getTitleEn());
    }

    @Test
    void testGeneralHelpGreeting() {
        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("");
        assertEquals("GREETING", resp.intent());
        assertTrue(resp.replyMarkdown().contains("LibraryOS AI Assistant"));
    }

    @Test
    void testBubtCseCourseRecommendation() {
        Book clrs = new Book();
        clrs.setId(56L);
        clrs.setTitleEn("Introduction to Algorithms");
        clrs.setAvailableCopies(4);

        when(catalogService.searchBooks("Algorithms")).thenReturn(List.of(clrs));

        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("Recommend books for CSE 209");
        assertEquals("COURSE_RECOMMENDATION", resp.intent());
        assertFalse(resp.bookStockDetails().isEmpty());
        assertTrue(resp.replyMarkdown().contains("CSE 209"));
        assertEquals("Introduction to Algorithms", resp.bookStockDetails().get(0).book().getTitleEn());
    }

    @Test
    void testAvailabilityInquiryInStock() {
        Book b = new Book();
        b.setId(1L);
        b.setTitleEn("Clean Code: A Handbook of Agile Software Craftsmanship");
        b.setAvailableCopies(3);
        b.setTotalCopies(5);

        BookCopy copy = new BookCopy();
        copy.setId(101L);
        copy.setBookId(1L);
        copy.setBarcode("BUBT-0001-0001");
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setShelfId(10L);

        Shelf shelf = new Shelf(10L, "R1-S1", "Science & Engineering", "Rack 1", "Level 1");

        when(catalogService.searchBooks(anyString())).thenReturn(List.of(b));
        when(bookCopyRepository.findByBookId(1L)).thenReturn(List.of(copy));
        when(shelfRepository.findById(10L)).thenReturn(Optional.of(shelf));

        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("Is Clean Code available?");
        assertEquals("AVAILABILITY_CHECK", resp.intent());
        assertTrue(resp.replyMarkdown().contains("IN STOCK"));
        assertTrue(resp.replyMarkdown().contains("R1-S1"));
        assertFalse(resp.bookStockDetails().isEmpty());
        assertTrue(resp.bookStockDetails().get(0).inStock());
        assertEquals("BUBT-0001-0001", resp.bookStockDetails().get(0).issueBarcode());
    }

    @Test
    void testAvailabilityInquiryOutOfStock() {
        Book b = new Book();
        b.setId(205L);
        b.setTitleEn("Computer Organization and Design");
        b.setAvailableCopies(0);
        b.setTotalCopies(3);

        BookCopy copy = new BookCopy();
        copy.setId(205L);
        copy.setBookId(205L);
        copy.setStatus(CopyStatus.ISSUED);

        when(catalogService.searchBooks(anyString())).thenReturn(List.of(b));
        when(bookCopyRepository.findByBookId(205L)).thenReturn(List.of(copy));

        AiAssistantService.AiResponse resp = aiAssistantService.processQuery("Is Computer Organization available?");
        assertEquals("AVAILABILITY_CHECK", resp.intent());
        assertTrue(resp.replyMarkdown().contains("OUT OF STOCK"));
        assertFalse(resp.bookStockDetails().get(0).inStock());
    }

    @Test
    void testLiveSuggestionsSearch() {
        Book b = new Book();
        b.setId(201L);
        b.setTitleEn("C Programming: A Modern Approach");
        b.setAvailableCopies(5);

        when(catalogService.searchBooks("C Programming")).thenReturn(List.of(b));

        List<AiAssistantService.BookStockInfo> suggestions = aiAssistantService.searchLiveSuggestions("CSE 111");
        assertFalse(suggestions.isEmpty());
        assertEquals("C Programming: A Modern Approach", suggestions.get(0).book().getTitleEn());
    }
}
