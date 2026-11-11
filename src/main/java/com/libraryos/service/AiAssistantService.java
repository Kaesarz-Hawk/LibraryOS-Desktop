package com.libraryos.service;

import com.libraryos.core.util.CurrencyFormatUtil;
import com.libraryos.model.catalog.Author;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.inventory.Shelf;
import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.repository.BookCopyRepository;
import com.libraryos.repository.BorrowingPolicyRepository;
import com.libraryos.repository.ShelfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 10x Intelligent AI Library Assistant & Live Book Recommendation Engine.
 * Supports:
 * - Natural language queries in English and Bengali (বাংলা)
 * - Context-aware BUBT CSE curriculum & course code matching (CSE 111, CSE 201, CSE 209, etc.)
 * - Real-time physical inventory stock & shelf location tracking
 * - Instant availability checks ("Is Clean Code available?")
 * - Interactive quick actions (Issue, View Details, Reserve)
 * - Safe advisory mode (read-only; never mutates financial or loan data directly)
 */
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    public record BookStockInfo(
            Book book,
            boolean inStock,
            int availableCopies,
            int totalCopies,
            String shelfLocation,
            String courseCodeTag,
            String issueBarcode
    ) {}

    public record AiResponse(
            String replyMarkdown,
            String intent,
            List<Book> relatedBooks,
            List<BookStockInfo> bookStockDetails
    ) {
        // Backward-compatible constructor for existing tests
        public AiResponse(String replyMarkdown, String intent, List<Book> relatedBooks) {
            this(replyMarkdown, intent, relatedBooks, List.of());
        }
    }

    private final CatalogService catalogService;
    private final RecommendationService recommendationService;
    private final BorrowingPolicyRepository borrowingPolicyRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ShelfRepository shelfRepository;

    // Cache of shelf ID -> Shelf object for ultra-fast lookup
    private final Map<Long, Shelf> shelfCache = new HashMap<>();

    public AiAssistantService(CatalogService catalogService,
                              RecommendationService recommendationService,
                              BorrowingPolicyRepository borrowingPolicyRepository) {
        this(catalogService, recommendationService, borrowingPolicyRepository, null, null);
    }

    public AiAssistantService(CatalogService catalogService,
                              RecommendationService recommendationService,
                              BorrowingPolicyRepository borrowingPolicyRepository,
                              BookCopyRepository bookCopyRepository,
                              ShelfRepository shelfRepository) {
        this.catalogService = catalogService;
        this.recommendationService = recommendationService;
        this.borrowingPolicyRepository = borrowingPolicyRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.shelfRepository = shelfRepository;
    }

    /**
     * Resolves live stock, available copy counts, barcode, and physical shelf location for a book.
     */
    public BookStockInfo resolveBookStock(Book book, String courseCodeTag) {
        if (book == null) return null;

        int total = book.getTotalCopies();
        int available = book.getAvailableCopies();
        String shelfLoc = "Central Stack Area";
        String barcode = null;
        boolean inStock = available > 0;

        if (bookCopyRepository != null) {
            try {
                List<BookCopy> copies = bookCopyRepository.findByBookId(book.getId());
                if (!copies.isEmpty()) {
                    total = copies.size();
                    List<BookCopy> availCopies = copies.stream()
                            .filter(c -> c.getStatus() == CopyStatus.AVAILABLE)
                            .toList();
                    available = availCopies.size();
                    inStock = available > 0;

                    BookCopy primaryCopy = inStock ? availCopies.get(0) : copies.get(0);
                    barcode = primaryCopy.getBarcode();

                    if (primaryCopy.getShelfId() != null) {
                        Shelf shelf = getCachedShelf(primaryCopy.getShelfId());
                        if (shelf != null) {
                            shelfLoc = String.format("%s (Rack: %s, Level: %s) • %s",
                                    shelf.getCode(), shelf.getRackNumber(), shelf.getShelfLevel(), shelf.getSectionName());
                        } else if (primaryCopy.getShelfCode() != null) {
                            shelfLoc = "Shelf " + primaryCopy.getShelfCode();
                        }
                    } else if (primaryCopy.getShelfCode() != null) {
                        shelfLoc = "Shelf " + primaryCopy.getShelfCode();
                    }
                }
            } catch (Exception e) {
                log.debug("Could not resolve copy details for book #{}: {}", book.getId(), e.getMessage());
            }
        }

        return new BookStockInfo(book, inStock, available, total, shelfLoc, courseCodeTag, barcode);
    }

    private Shelf getCachedShelf(Long shelfId) {
        if (shelfId == null) return null;
        if (shelfCache.containsKey(shelfId)) return shelfCache.get(shelfId);
        if (shelfRepository != null) {
            try {
                Optional<Shelf> s = shelfRepository.findById(shelfId);
                s.ifPresent(shelf -> shelfCache.put(shelfId, shelf));
                return s.orElse(null);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Live search for the top header autocomplete bar.
     * Returns matching books enriched with real-time stock and shelf locations.
     */
    public List<BookStockInfo> searchLiveSuggestions(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        String clean = query.trim();

        // 1. Check if query matches a BUBT CSE course code
        String courseMatch = matchBubtCseCourse(clean);
        List<Book> books;
        if (courseMatch != null) {
            books = findBooksForCourse(courseMatch);
        } else {
            books = catalogService.searchBooks(clean);
        }

        return books.stream()
                .limit(6)
                .map(b -> resolveBookStock(b, courseMatch))
                .collect(Collectors.toList());
    }

    /**
     * Processes natural language inquiries from library patrons and staff.
     */
    public AiResponse processQuery(String userQuery) {
        if (userQuery == null || userQuery.trim().isBlank()) {
            return new AiResponse(
                    "Hello! I am your **LibraryOS AI Assistant** 🤖 *(BUBT CSE Edition)*.\n\n" +
                    "Ask me anything in **English** or **বাংলা**:\n" +
                    "- 📚 *\"Recommend books for Data Structures\"* or *\"Books for CSE 201\"*\n" +
                    "- 🔍 *\"Is Clean Code available?\"* or *\"Operating Systems book shelf location?\"*\n" +
                    "- ৳ *\"What is the overdue fine rate?\"* / *\"দেরিতে জমা দিলে জরিমানা কত?\"*\n" +
                    "- 📜 *\"How many books can a student borrow?\"*",
                    "GREETING", List.of(), List.of());
        }

        String q = userQuery.trim().toLowerCase();

        // 1. Instant Availability Check ("Is Clean Code available?", "Have you got CLRS in stock?", "বইটি কি আছে?")
        if (isAvailabilityInquiry(q)) {
            return handleAvailabilityInquiry(userQuery);
        }

        // 2. BUBT CSE Course Code Specific Inquiries ("CSE 209", "CSE 111 books", "Books for CSE 303")
        String courseCode = matchBubtCseCourse(q);
        if (courseCode != null) {
            return handleCourseInquiry(courseCode, userQuery);
        }

        // 3. Overdue fines & financial inquiries
        if (q.contains("fine") || q.contains("জরিমানা") || q.contains("ফি") || q.contains("overdue") || q.contains("late")) {
            return handleFineInquiry();
        }

        // 4. Borrowing limits, duration, and policies
        if (q.contains("limit") || q.contains("how many") || q.contains("কয়টি") || q.contains("কয়টা") ||
            q.contains("duration") || q.contains("loan") || q.contains("ধার") || q.contains("নিয়ম") || q.contains("policy")) {
            return handlePolicyInquiry();
        }

        // 5. Reservations & Holds
        if (q.contains("reservation") || q.contains("reserve") || q.contains("সংরক্ষণ") || q.contains("hold") || q.contains("অপেক্ষা")) {
            return handleReservationInquiry();
        }

        // 6. Recommendations & subject suggestions ("Data Structures", "Algorithms", "Operating Systems", "Python", etc.)
        if (q.contains("recommend") || q.contains("suggest") || q.contains("জনপ্রিয়") || q.contains("পরামর্শ") ||
            q.contains("popular") || q.contains("best") || isSubjectQuery(q)) {
            return handleSubjectOrGeneralRecommendation(userQuery);
        }

        // 7. General Book Search
        if (q.contains("find") || q.contains("search") || q.contains("খুঁজ") || q.contains("বই") || q.contains("book") || q.contains("author") || q.contains("লেখক")) {
            return handleBookSearchInquiry(userQuery);
        }

        // 8. Fallback to smart search or general help
        List<Book> fallbackSearch = catalogService.searchBooks(userQuery.trim());
        if (!fallbackSearch.isEmpty()) {
            return buildBookListResponse("### 🔍 Library Catalog Search Results\n\nFound matching titles in our collection:",
                    fallbackSearch.subList(0, Math.min(fallbackSearch.size(), 5)), "CATALOG_SEARCH", null);
        }

        return handleGeneralHelp(userQuery);
    }

    private boolean isAvailabilityInquiry(String q) {
        return q.contains("available") || q.contains("in stock") || q.contains("stock") ||
               q.contains("আছে কি") || q.contains("পাওয়া যাবে") || q.contains("is there") ||
               (q.startsWith("is ") && (q.contains("available") || q.contains("there") || q.contains("in library")));
    }

    private AiResponse handleAvailabilityInquiry(String rawQuery) {
        String clean = rawQuery.replaceAll("(?i)(is|are|the|book|available|in stock|stock|here|please|tell me|আছে|কি|পাওয়া যাবে|খুঁজে দিন|do you have)", "").trim();
        if (clean.isBlank()) {
            clean = rawQuery;
        }

        List<Book> books = catalogService.searchBooks(clean);
        if (books.isEmpty()) {
            return new AiResponse(
                    String.format("### 🔍 Availability Check\n\nI could not find any book title matching **\"%s\"** in our library catalog.\n\nPlease check the spelling or search by author/ISBN in the Books Catalog tab.", clean),
                    "AVAILABILITY_CHECK", List.of(), List.of()
            );
        }

        Book bestMatch = books.get(0);
        BookStockInfo stock = resolveBookStock(bestMatch, null);

        StringBuilder sb = new StringBuilder();
        sb.append("### 🔍 Live Stock & Availability Status\n\n");
        if (stock.inStock()) {
            sb.append(String.format("🟢 **IN STOCK — Ready for Issue!**\n\n"));
            sb.append(String.format("- **Title**: **%s**\n", bestMatch.getTitle()));
            sb.append(String.format("- **Available Copies**: **%d** of %d total\n", stock.availableCopies(), stock.totalCopies()));
            sb.append(String.format("- **Shelf Location**: 📍 `%s`\n", stock.shelfLocation()));
            sb.append("- **Status**: You can issue this book immediately at the Circulation Desk.\n");
        } else {
            sb.append(String.format("🔴 **CURRENTLY OUT OF STOCK**\n\n"));
            sb.append(String.format("- **Title**: **%s**\n", bestMatch.getTitle()));
            sb.append(String.format("- **Available Copies**: **0** (All %d copies are currently checked out)\n", stock.totalCopies()));
            sb.append("- **Action**: You can place a **FIFO Hold Reservation** to automatically secure the next returned copy!\n");
        }

        return new AiResponse(sb.toString(), "AVAILABILITY_CHECK", List.of(bestMatch), List.of(stock));
    }

    private boolean isSubjectQuery(String q) {
        return q.contains("data structure") || q.contains("algorithm") || q.contains("database") ||
               q.contains("operating system") || q.contains("network") || q.contains("compiler") ||
               q.contains("clean code") || q.contains("machine learning") || q.contains("artificial intelligence") ||
               q.contains("python") || q.contains("c++") || q.contains("software engineering") || q.contains("architecture");
    }

    private String matchBubtCseCourse(String text) {
        Pattern p = Pattern.compile("(?i)\\b(CSE[\\s-]?\\d{3})\\b");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).toUpperCase().replaceAll("\\s+", " ").replace("-", " ");
        }

        String lower = text.toLowerCase();
        if (lower.contains("data structure")) return "CSE 201";
        if (lower.contains("algorithm")) return "CSE 209";
        if (lower.contains("database") || lower.contains("dbms")) return "CSE 301";
        if (lower.contains("operating system")) return "CSE 303";
        if (lower.contains("software engineering")) return "CSE 315";
        if (lower.contains("computer network") || lower.contains("networking")) return "CSE 325";
        if (lower.contains("compiler")) return "CSE 417";
        if (lower.contains("artificial intelligence") || lower.contains("machine learning")) return "CSE 401";
        if (lower.contains("discrete math")) return "CSE 123";
        if (lower.contains("c programming") || lower.contains("structured programming")) return "CSE 111";
        if (lower.contains("c++") || lower.contains("oop") || lower.contains("object oriented")) return "CSE 121";

        return null;
    }

    private List<Book> findBooksForCourse(String courseCode) {
        String query;
        switch (courseCode.toUpperCase().replaceAll("\\s+", "")) {
            case "CSE111", "CSE112" -> query = "C Programming";
            case "CSE121", "CSE122" -> query = "C++";
            case "CSE123", "CSE124" -> query = "Discrete Mathematics";
            case "CSE201", "CSE207", "CSE208" -> query = "Data Structures";
            case "CSE209", "CSE245" -> query = "Algorithms";
            case "CSE225" -> query = "Digital";
            case "CSE231" -> query = "Computer Organization";
            case "CSE301", "CSE311" -> query = "Database";
            case "CSE303", "CSE323" -> query = "Operating Systems";
            case "CSE315" -> query = "Clean Code";
            case "CSE325" -> query = "Networks";
            case "CSE401", "CSE405" -> query = "Artificial Intelligence";
            case "CSE417" -> query = "Compilers";
            default -> query = courseCode;
        }
        return catalogService.searchBooks(query);
    }

    private AiResponse handleCourseInquiry(String courseCode, String userQuery) {
        List<Book> books = findBooksForCourse(courseCode);
        if (books.isEmpty()) {
            books = catalogService.searchBooks("Computer Science");
        }

        String courseName = getCourseName(courseCode);
        String header = String.format("### 🎓 Recommended Books for %s: %s\n\n" +
                "Here are the core textbook and reference volumes available in the BUBT Central Library:",
                courseCode, courseName);

        return buildBookListResponse(header, books.subList(0, Math.min(books.size(), 4)), "COURSE_RECOMMENDATION", courseCode);
    }

    private String getCourseName(String code) {
        return switch (code.toUpperCase().replaceAll("\\s+", "")) {
            case "CSE111" -> "Structured Programming Language (C)";
            case "CSE121" -> "Object Oriented Programming (C++)";
            case "CSE123" -> "Discrete Mathematics";
            case "CSE201", "CSE207" -> "Data Structures & Algorithms I";
            case "CSE209", "CSE245" -> "Algorithm Design & Analysis";
            case "CSE225" -> "Digital Logic Design";
            case "CSE231" -> "Computer Architecture & Organization";
            case "CSE301", "CSE311" -> "Database Management Systems (DBMS)";
            case "CSE303", "CSE323" -> "Operating Systems";
            case "CSE315" -> "Software Engineering & System Architecture";
            case "CSE325" -> "Data Communication & Computer Networks";
            case "CSE401", "CSE405" -> "Artificial Intelligence & Machine Learning";
            case "CSE417" -> "Compiler Design & Construction";
            default -> "BUBT CSE Academic Course";
        };
    }

    private AiResponse handleSubjectOrGeneralRecommendation(String userQuery) {
        String clean = userQuery.replaceAll("(?i)(recommend|suggest|popular|best|books|for|about|পরামর্শ|সুপারিশ)", "").trim();
        List<Book> books;
        if (!clean.isBlank() && clean.length() > 2) {
            books = catalogService.searchBooks(clean);
        } else {
            books = recommendationService != null ? recommendationService.getPopularBooks(4) : List.of();
        }

        if (books.isEmpty()) {
            books = catalogService.getAllBooks().stream().limit(4).toList();
        }

        String header = clean.isBlank() ?
                "### ⭐ Most Popular & Recommended Books\n\nTop recommended selections based on patron borrowing frequency and ratings:" :
                String.format("### 📚 Smart Recommendations for \"%s\"\n\nHere are the top matches curated from the institutional catalog:", clean);

        return buildBookListResponse(header, books.subList(0, Math.min(books.size(), 4)), "RECOMMENDATIONS", null);
    }

    private AiResponse handleBookSearchInquiry(String rawQuery) {
        String clean = rawQuery.replaceAll("(?i)(find|search|book|books|খুঁজুন|বই|দাও|দেখান|please|where is)", "").trim();
        List<Book> results = catalogService.searchBooks(clean);

        if (results.isEmpty()) {
            return new AiResponse("I could not find any books matching `\"" + clean + "\"`. Try searching with a broader title, Bangla keyword, or author name in the Catalog screen.",
                    "CATALOG_SEARCH", List.of(), List.of());
        }

        return buildBookListResponse(
                String.format("### 🔍 Search Results for `\"%s\"`\n\nFound **%d** title(s) in catalog:", clean, results.size()),
                results.subList(0, Math.min(results.size(), 4)),
                "CATALOG_SEARCH",
                null
        );
    }

    private AiResponse buildBookListResponse(String markdownHeader, List<Book> books, String intent, String courseTag) {
        StringBuilder sb = new StringBuilder();
        sb.append(markdownHeader).append("\n\n");

        List<BookStockInfo> stocks = new ArrayList<>();
        for (int i = 0; i < books.size(); i++) {
            Book b = books.get(i);
            BookStockInfo stock = resolveBookStock(b, courseTag);
            stocks.add(stock);

            String author = (b.getAuthors() != null && !b.getAuthors().isEmpty()) ?
                    b.getAuthors().get(0).getNameEn() : "Academic Reference";
            String titleBn = (b.getTitleBn() != null && !b.getTitleBn().isBlank()) ? " (" + b.getTitleBn() + ")" : "";

            sb.append(String.format("%d. **%s%s** by *%s*\n", (i + 1), b.getTitle(), titleBn, author));
            if (stock.inStock()) {
                sb.append(String.format("   - 🟢 **In Stock**: `%d copies available` • 📍 **%s**\n", stock.availableCopies(), stock.shelfLocation()));
            } else {
                sb.append(String.format("   - 🔴 **Out of Stock**: All %d copies loaned • *Reservation queue open*\n", stock.totalCopies()));
            }
        }

        return new AiResponse(sb.toString(), intent, books, stocks);
    }

    private AiResponse handleFineInquiry() {
        StringBuilder sb = new StringBuilder();
        sb.append("### ৳ Overdue Fine Policy / জরিমানা নীতিমালা\n\n");
        sb.append("- **Standard Overdue Rate**: **৳ 5.00** per day past the loan due date.\n");
        sb.append("- **Grace Period**: 0 to 2 days (dependent on institutional member type).\n");
        sb.append("- **Weekly Holidays**: Friday weekly holidays are automatically excluded in overdue calculations unless specified.\n");
        sb.append("- **Payment Methods**: Fines can be settled in Cash, bKash, Nagad, or Rocket at the Finance Desk.\n\n");
        sb.append("*(বাংলা)*: বই নির্ধারিত তারিখের পর জমা দিলে প্রতিদিন ৫ টাকা জরিমানা হিসাব করা হয়। শুক্রবার সাপ্তাহিক ছুটির দিন জরিমানার দিনের সাথে যোগ হয় না।");
        return new AiResponse(sb.toString(), "POLICY_FINES", List.of(), List.of());
    }

    private AiResponse handlePolicyInquiry() {
        StringBuilder sb = new StringBuilder();
        sb.append("### 📚 Institutional Borrowing Rules / বই ধার নেওয়ার নিয়ম\n\n");

        if (borrowingPolicyRepository != null) {
            List<BorrowingPolicy> policies = borrowingPolicyRepository.findAll();
            for (BorrowingPolicy p : policies) {
                sb.append(String.format("- **%s**: Up to **%d books** for **%d days** (Max renewals: %d, Fine: %s/day)\n",
                        p.getMemberTypeCode() != null ? p.getMemberTypeCode() : "Member Type #" + p.getMemberTypeId(),
                        p.getMaxActiveLoans(),
                        p.getLoanDurationDays(),
                        p.getMaxRenewals(),
                        CurrencyFormatUtil.format(p.getFinePerDayBdt())));
            }
        } else {
            sb.append("- **Students**: Up to 3 books for 14 days (1 renewal).\n");
            sb.append("- **Faculty / Teachers**: Up to 10 books for 30 days (3 renewals).\n");
            sb.append("- **Staff**: Up to 3 books for 14 days (1 renewal).\n");
        }
        sb.append("\n*(বাংলা)*: সাধারণ শিক্ষার্থীরা একসাথে সর্বোচ্চ ৩টি বই ১৪ দিনের জন্য নিতে পারেন। শিক্ষকরা সর্বোচ্চ ১০টি বই ৩০ দিনের জন্য নিতে পারেন।");
        return new AiResponse(sb.toString(), "POLICY_BORROWING", List.of(), List.of());
    }

    private AiResponse handleReservationInquiry() {
        String reply = "### ⏳ Reservation & Hold Rules / বই সংরক্ষণের নিয়ম\n\n" +
                "1. If all physical copies of a book title are currently issued, any active member may place a **Hold Reservation**.\n" +
                "2. Reservations operate strictly on a **First-In, First-Out (FIFO)** queue basis.\n" +
                "3. When a copy is returned, the system automatically allocates it to the first member in line and marks status as `READY_FOR_PICKUP`.\n" +
                "4. The member has **3 business days** to collect the book before the hold expires and progresses to the next person.\n\n" +
                "*(বাংলা)*: বইয়ের সব কপি ধার দেওয়া থাকলে রিজার্ভেশন করা যায়। বই ফেরত আসা মাত্রই সিরিয়াল অনুযায়ী প্রথম ব্যক্তিকে ৩ দিনের জন্য বরাদ্দ দেওয়া হয়।";
        return new AiResponse(reply, "POLICY_RESERVATIONS", List.of(), List.of());
    }

    private AiResponse handleGeneralHelp(String rawQuery) {
        String reply = "### 🤖 LibraryOS Assistant Help *(BUBT CSE Edition)*\n\n" +
                "I can assist you with:\n" +
                "- **BUBT CSE Course Books**: e.g. *\"Books for CSE 201\"* or *\"Recommend books for Operating Systems\"*\n" +
                "- **Instant Stock & Availability**: e.g. *\"Is Clean Code available?\"* or *\"Where is Silberschatz book placed?\"*\n" +
                "- **Borrowing Rules**: e.g. *\"How many books can a student borrow?\"*\n" +
                "- **Overdue Fines**: e.g. *\"What is the fine per day?\"* (৳ 5.00/day)\n" +
                "- **Digital Library**: e.g. *\"How to read PDF online?\"*\n\n" +
                "*(বাংলা)*: বিইউবিটি সিএসই কোর্সের বই, লাইব্রেরি স্টক, জরিমানা বা যেকোনো নীতিমালার জন্য প্রশ্ন করতে পারেন।";
        return new AiResponse(reply, "GENERAL_HELP", List.of(), List.of());
    }
}
