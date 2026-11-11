package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.validation.ValidationUtil;
import com.libraryos.model.catalog.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuditService auditService;

    public CatalogService(BookRepository bookRepository,
                          AuthorRepository authorRepository,
                          CategoryRepository categoryRepository,
                          PublisherRepository publisherRepository,
                          AuditService auditService) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.auditService = auditService;
    }

    public List<Book> getAllBooks(boolean includeArchived) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return bookRepository.findAll(includeArchived);
    }

    public List<Book> getAllBooks() {
        return getAllBooks(false);
    }

    public List<Book> searchBooks(String query, Long categoryId, Long publisherId) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return bookRepository.search(query, categoryId, publisherId);
    }

    public List<Book> searchBooks(String query) {
        return searchBooks(query, null, null);
    }

    public Optional<Book> getBookById(Long id) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return bookRepository.findById(id);
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return bookRepository.findByIsbn(isbn);
    }

    public Book createBook(Book book, List<Long> authorIds, List<Long> categoryIds) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_CREATE);
        validateBook(book);

        // Check duplicate ISBN
        if (book.getIsbn13() != null && !book.getIsbn13().isBlank()) {
            bookRepository.findByIsbn(book.getIsbn13()).ifPresent(existing -> {
                throw new ValidationException("A book title with ISBN-13 '" + book.getIsbn13() + "' already exists: " + existing.getTitleEn());
            });
        }

        Book created = bookRepository.create(book);

        if (authorIds != null && !authorIds.isEmpty()) {
            authorRepository.setBookAuthors(created.getId(), authorIds);
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            categoryRepository.setBookCategories(created.getId(), categoryIds);
        }

        auditService.log("BOOK_CREATED", "BOOK", String.valueOf(created.getId()),
                "Created book title: " + created.getTitleEn(), "Catalog addition");
        log.info("Book title created: #{} - {}", created.getId(), created.getTitleEn());
        return getBookById(created.getId()).orElse(created);
    }

    public void updateBook(Book book, List<Long> authorIds, List<Long> categoryIds) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_UPDATE);
        if (book.getId() == null) {
            throw new ValidationException("Book ID must be specified for update.");
        }
        validateBook(book);

        bookRepository.update(book);

        if (authorIds != null) {
            authorRepository.setBookAuthors(book.getId(), authorIds);
        }
        if (categoryIds != null) {
            categoryRepository.setBookCategories(book.getId(), categoryIds);
        }

        auditService.log("BOOK_UPDATED", "BOOK", String.valueOf(book.getId()),
                "Updated book title: " + book.getTitleEn(), "Catalog edit");
        log.info("Book title updated: #{} - {}", book.getId(), book.getTitleEn());
    }

    public void archiveBook(Long bookId) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_ARCHIVE);
        bookRepository.archive(bookId);
        auditService.log("BOOK_ARCHIVED", "BOOK", String.valueOf(bookId),
                "Archived book ID: " + bookId, "Catalog archival");
        log.info("Book title archived: #{}", bookId);
    }

    private void validateBook(Book book) {
        List<String> errors = new ArrayList<>();
        if (book.getTitleEn() == null || book.getTitleEn().isBlank()) {
            errors.add("English Title is mandatory.");
        }
        if (book.getIsbn10() != null && !book.getIsbn10().isBlank() && !ValidationUtil.isValidIsbn10(book.getIsbn10())) {
            errors.add("Invalid ISBN-10 checksum.");
        }
        if (book.getIsbn13() != null && !book.getIsbn13().isBlank() && !ValidationUtil.isValidIsbn13(book.getIsbn13())) {
            errors.add("Invalid ISBN-13 checksum.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    // Authors, Categories, Publishers
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    public Author createAuthor(Author author) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_CREATE);
        if (author.getNameEn() == null || author.getNameEn().isBlank()) {
            throw new ValidationException("Author English name is mandatory.");
        }
        Author created = authorRepository.create(author);
        auditService.log("AUTHOR_CREATED", "AUTHOR", String.valueOf(created.getId()),
                "Created author: " + created.getNameEn(), "Catalog setup");
        return created;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(Category category) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_CREATE);
        if (category.getNameEn() == null || category.getNameEn().isBlank()) {
            throw new ValidationException("Category English name is mandatory.");
        }
        Category created = categoryRepository.create(category);
        auditService.log("CATEGORY_CREATED", "CATEGORY", String.valueOf(created.getId()),
                "Created category: " + created.getNameEn(), "Catalog setup");
        return created;
    }

    public List<Publisher> getAllPublishers() {
        return publisherRepository.findAll();
    }

    public Publisher createPublisher(Publisher publisher) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_CREATE);
        if (publisher.getName() == null || publisher.getName().isBlank()) {
            throw new ValidationException("Publisher name is mandatory.");
        }
        Publisher created = publisherRepository.create(publisher);
        auditService.log("PUBLISHER_CREATED", "PUBLISHER", String.valueOf(created.getId()),
                "Created publisher: " + created.getName(), "Catalog setup");
        return created;
    }
}

