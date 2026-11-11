package com.libraryos.service;

import com.libraryos.config.FileStorageConfig;
import com.libraryos.core.exception.DomainException;
import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.digital.Bookmark;
import com.libraryos.model.digital.DigitalBook;
import com.libraryos.model.digital.ReadingProgress;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.BookmarkRepository;
import com.libraryos.repository.DigitalBookRepository;
import com.libraryos.repository.ReadingProgressRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DigitalLibraryService {

    private static final Logger log = LoggerFactory.getLogger(DigitalLibraryService.class);

    private final DigitalBookRepository digitalBookRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final BookmarkRepository bookmarkRepository;
    private final AuditService auditService;

    public DigitalLibraryService(DigitalBookRepository digitalBookRepository,
                                 ReadingProgressRepository readingProgressRepository,
                                 BookmarkRepository bookmarkRepository,
                                 AuditService auditService) {
        this.digitalBookRepository = digitalBookRepository;
        this.readingProgressRepository = readingProgressRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.auditService = auditService;
    }

    public List<DigitalBook> getAllDigitalBooks() {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return digitalBookRepository.findAll();
    }

    public List<DigitalBook> searchDigitalBooks(String query) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return digitalBookRepository.search(query);
    }

    public Optional<DigitalBook> getDigitalBookById(Long id) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return digitalBookRepository.findById(id);
    }

    public Optional<DigitalBook> getDigitalBookByBookId(Long bookId) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return digitalBookRepository.findByBookId(bookId);
    }

    /**
     * Uploads and registers a digital PDF book into managed local storage.
     */
    public DigitalBook uploadDigitalBook(Long bookId, File sourcePdfFile, boolean downloadAllowed) throws IOException {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_MANAGE);

        if (sourcePdfFile == null || !sourcePdfFile.exists() || !sourcePdfFile.canRead()) {
            throw new DomainException("INVALID_FILE", "The specified source PDF file does not exist or cannot be read.");
        }

        // Validate PDF header magic bytes "%PDF-"
        validatePdfMagicBytes(sourcePdfFile);

        // Ensure managed storage directory exists
        FileStorageConfig.initializeDirectories();
        String digitalDir = FileStorageConfig.getDigitalDir();

        // Calculate SHA-256 Checksum
        String checksum = calculateSha256(sourcePdfFile);

        // Count pages via PDFBox
        int totalPages = countPdfPages(sourcePdfFile);

        // Generate safe unique target filename
        String safeName = UUID.randomUUID().toString() + "_" + sanitizeFileName(sourcePdfFile.getName());
        Path targetPath = Path.of(digitalDir, safeName);
        Files.copy(sourcePdfFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        UserSession session = SessionManager.getCurrentSession().orElse(null);
        Long uploaderId = (session != null && session.getUserId() != null) ? session.getUserId() : 1L;

        DigitalBook digitalBook = new DigitalBook();
        digitalBook.setBookId(bookId);
        digitalBook.setFileName(sourcePdfFile.getName());
        digitalBook.setFilePath(targetPath.toAbsolutePath().toString());
        digitalBook.setFileSizeBytes(Files.size(targetPath));
        digitalBook.setMimeType("application/pdf");
        digitalBook.setSha256Checksum(checksum);
        digitalBook.setTotalPages(totalPages);
        digitalBook.setDownloadAllowed(downloadAllowed);
        digitalBook.setUploadedBy(uploaderId);
        digitalBook.setUploadedAt(LocalDateTime.now());

        DigitalBook saved = digitalBookRepository.save(digitalBook);

        if (auditService != null) {
            auditService.log("DIGITAL_BOOK_UPLOAD", "DigitalBook",
                    String.valueOf(saved.getId()),
                    "Uploaded digital PDF for book ID " + bookId + " (" + totalPages + " pages)",
                    null);
        }

        log.info("Successfully uploaded digital book: {} ({} pages, {})", saved.getFileName(), totalPages, targetPath);
        return saved;
    }

    /**
     * Renders a specific page of a PDF document to PNG byte array.
     * Page index is 0-based.
     */
    public byte[] renderPageToPng(Long digitalBookId, int pageIndex, float dpi) throws IOException {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);

        DigitalBook book = digitalBookRepository.findById(digitalBookId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Digital book not found: " + digitalBookId));

        File file = new File(book.getFilePath());
        if (!file.exists()) {
            throw new DomainException("FILE_NOT_FOUND", "The PDF file could not be found at: " + book.getFilePath());
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            int pageCount = document.getNumberOfPages();
            if (pageIndex < 0 || pageIndex >= pageCount) {
                throw new DomainException("PAGE_OUT_OF_BOUNDS", "Requested page index " + pageIndex + " is out of bounds (1-" + pageCount + ").");
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bim = renderer.renderImageWithDPI(pageIndex, dpi > 0 ? dpi : 140f, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bim, "PNG", baos);
            return baos.toByteArray();
        }
    }

    /**
     * Extracts plain text from a specific 1-based page of the PDF.
     */
    public String extractPageText(Long digitalBookId, int pageNumber) throws IOException {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);

        DigitalBook book = digitalBookRepository.findById(digitalBookId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Digital book not found: " + digitalBookId));

        File file = new File(book.getFilePath());
        if (!file.exists()) {
            return "";
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            return stripper.getText(document);
        }
    }

    /**
     * Saves or updates a member's reading progress.
     */
    public void recordReadingProgress(Long memberId, Long digitalBookId, int currentPage, int totalPages) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);

        // Member isolation: if member role, must match own member id
        UserSession session = SessionManager.getCurrentSession().orElse(null);
        if (session != null && session.isMemberOnly() && session.getMemberId() != null) {
            if (!session.getMemberId().equals(memberId)) {
                throw new UnauthorizedException("Members may only track their own reading progress.");
            }
        }

        ReadingProgress rp = new ReadingProgress(memberId, digitalBookId, Math.max(1, currentPage), Math.max(1, totalPages));
        readingProgressRepository.saveOrUpdate(rp);
    }

    public Optional<ReadingProgress> getReadingProgress(Long memberId, Long digitalBookId) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return readingProgressRepository.findByMemberAndBook(memberId, digitalBookId);
    }

    public List<ReadingProgress> getMemberReadingProgress(Long memberId) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return readingProgressRepository.findByMemberId(memberId);
    }

    // Bookmarks Management
    public List<Bookmark> getBookmarks(Long memberId, Long digitalBookId) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return bookmarkRepository.findByMemberAndBook(memberId, digitalBookId);
    }

    public Bookmark addBookmark(Long memberId, Long digitalBookId, int pageNumber, String title, String note) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        Bookmark bm = new Bookmark(memberId, digitalBookId, pageNumber, title, note);
        return bookmarkRepository.save(bm);
    }

    public boolean removeBookmark(Long bookmarkId, Long memberId) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_READ);
        return bookmarkRepository.delete(bookmarkId, memberId);
    }

    public boolean deleteDigitalBook(Long id) {
        PermissionGuard.requirePermission(PermissionCode.DIGITAL_MANAGE);
        Optional<DigitalBook> bookOpt = digitalBookRepository.findById(id);
        if (bookOpt.isPresent()) {
            DigitalBook book = bookOpt.get();
            try {
                Files.deleteIfExists(Path.of(book.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete physical file: {}", book.getFilePath(), e);
            }
            return digitalBookRepository.delete(id);
        }
        return false;
    }

    // Utility helpers
    private void validatePdfMagicBytes(File file) {
        try (InputStream in = new FileInputStream(file)) {
            byte[] header = new byte[5];
            int read = in.read(header);
            if (read < 5 || !"%PDF-".equals(new String(header, 0, 5))) {
                throw new DomainException("INVALID_PDF_FORMAT", "The uploaded file is not a valid PDF document (missing %PDF- header).");
            }
        } catch (IOException e) {
            throw new DomainException("FILE_READ_ERROR", "Failed to inspect file header: " + e.getMessage());
        }
    }

    private String calculateSha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Failed to calculate SHA-256 checksum", e);
        }
    }

    private int countPdfPages(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return doc.getNumberOfPages();
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "document.pdf";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
