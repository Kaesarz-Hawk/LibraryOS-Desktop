package com.libraryos.app;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.config.FileStorageConfig;
import com.libraryos.repository.*;
import com.libraryos.repository.impl.*;
import com.libraryos.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Dependency container and application lifecycle manager for LibraryOS.
 */
public final class AppContext {

    private static final Logger log = LoggerFactory.getLogger(AppContext.class);

    private static volatile AppContext instance;

    // Concurrency Executor for JavaFX background tasks
    private final ExecutorService executorService;

    // Phase 1 Repositories
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AppSettingRepository appSettingRepository;
    private final DashboardRepository dashboardRepository;

    // Phase 2 Repositories
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final ShelfRepository shelfRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;
    private final BorrowingPolicyRepository borrowingPolicyRepository;

    // Phase 3 & 4 Repositories
    private final LoanRepository loanRepository;
    private final LoanRenewalRepository loanRenewalRepository;
    private final FineRepository fineRepository;
    private final FineAdjustmentRepository fineAdjustmentRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    // Phase 5 Repositories & Services
    private final ReportRepository reportRepository;
    private final BarcodeService barcodeService;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;
    private final ReportService reportService;
    private final BackupService backupService;

    // Phase 6 Repositories & Services
    private final SupplierRepository supplierRepository;
    private final AcquisitionRepository acquisitionRepository;
    private final DigitalBookRepository digitalBookRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    private final DigitalLibraryService digitalLibraryService;
    private final AcquisitionService acquisitionService;
    private final ReviewService reviewService;
    private final NotificationService notificationService;

    // Phase 7 AI & Advanced Integrations
    private final RecommendationService recommendationService;
    private final MfsVerificationService mfsVerificationService;
    private final NotificationGatewayService notificationGatewayService;
    private final AiAssistantService aiAssistantService;

    // Services
    private final AuthService authService;
    private final AuditService auditService;
    private final SettingService settingService;
    private final DashboardService dashboardService;
    private final CatalogService catalogService;
    private final InventoryService inventoryService;
    private final MemberService memberService;
    private final FineCalculationService fineCalculationService;
    private final CirculationService circulationService;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final PaymentService paymentService;

    private AppContext() {
        log.info("Initializing LibraryOS Application Context...");

        // 1. Thread pool for background tasks
        this.executorService = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "LibraryOS-Worker-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });

        // 2. Storage directories
        FileStorageConfig.initializeDirectories();

        // 3. Database connection & Flyway migrations
        DatabaseConfig.initialize();

        // 4. Repositories
        this.permissionRepository = new JdbcPermissionRepository();
        this.roleRepository = new JdbcRoleRepository(this.permissionRepository);
        this.userRepository = new JdbcUserRepository(this.roleRepository);
        this.auditLogRepository = new JdbcAuditLogRepository();
        this.appSettingRepository = new JdbcAppSettingRepository();
        this.dashboardRepository = new JdbcDashboardRepository();

        this.publisherRepository = new JdbcPublisherRepository();
        this.authorRepository = new JdbcAuthorRepository();
        this.categoryRepository = new JdbcCategoryRepository();
        this.shelfRepository = new JdbcShelfRepository();
        this.bookRepository = new JdbcBookRepository(this.authorRepository, this.categoryRepository);
        this.bookCopyRepository = new JdbcBookCopyRepository();
        this.memberRepository = new JdbcMemberRepository();
        this.borrowingPolicyRepository = new JdbcBorrowingPolicyRepository();

        this.loanRepository = new JdbcLoanRepository();
        this.loanRenewalRepository = new JdbcLoanRenewalRepository();
        this.fineRepository = new JdbcFineRepository();
        this.fineAdjustmentRepository = new JdbcFineAdjustmentRepository();
        this.paymentRepository = new JdbcPaymentRepository();
        this.reservationRepository = new JdbcReservationRepository();

        // 5. Services
        this.auditService = new AuditService(this.auditLogRepository);
        this.authService = new AuthService(this.userRepository, this.permissionRepository, this.auditLogRepository);
        this.settingService = new SettingService(this.appSettingRepository);
        this.dashboardService = new DashboardService(this.dashboardRepository);

        this.catalogService = new CatalogService(this.bookRepository, this.authorRepository, this.categoryRepository, this.publisherRepository, this.auditService);
        this.inventoryService = new InventoryService(this.bookCopyRepository, this.shelfRepository, this.auditService);
        this.memberService = new MemberService(this.memberRepository, this.borrowingPolicyRepository, this.auditService);

        this.fineCalculationService = new FineCalculationService();
        this.circulationService = new CirculationService(
                this.loanRepository,
                this.loanRenewalRepository,
                this.bookCopyRepository,
                this.memberRepository,
                this.borrowingPolicyRepository,
                this.fineRepository,
                this.reservationRepository,
                this.fineCalculationService,
                this.auditService
        );

        this.reservationService = new ReservationService(
                this.reservationRepository,
                this.bookRepository,
                this.bookCopyRepository,
                this.memberRepository,
                this.borrowingPolicyRepository,
                this.loanRepository,
                this.fineRepository,
                this.auditService
        );

        this.fineService = new FineService(
                this.fineRepository,
                this.fineAdjustmentRepository,
                this.memberRepository,
                this.auditService
        );

        this.paymentService = new PaymentService(
                this.paymentRepository,
                this.fineRepository,
                this.memberRepository,
                this.auditService
        );

        // 6. Phase 5 Services
        this.reportRepository = new JdbcReportRepository();
        this.barcodeService = new BarcodeService(this.auditService);
        this.pdfExportService = new PdfExportService();
        this.excelExportService = new ExcelExportService();
        this.reportService = new ReportService(this.reportRepository, this.pdfExportService, this.excelExportService);
        this.backupService = new BackupService(this.auditService);

        // 7. Phase 6 Repositories & Services
        this.supplierRepository = new JdbcSupplierRepository();
        this.acquisitionRepository = new JdbcAcquisitionRepository();
        this.digitalBookRepository = new JdbcDigitalBookRepository();
        this.readingProgressRepository = new JdbcReadingProgressRepository();
        this.bookmarkRepository = new JdbcBookmarkRepository();
        this.reviewRepository = new JdbcReviewRepository();
        this.notificationRepository = new JdbcNotificationRepository();

        this.digitalLibraryService = new DigitalLibraryService(
                this.digitalBookRepository,
                this.readingProgressRepository,
                this.bookmarkRepository,
                this.auditService
        );
        this.acquisitionService = new AcquisitionService(
                this.supplierRepository,
                this.acquisitionRepository,
                this.bookCopyRepository,
                this.auditService
        );
        this.reviewService = new ReviewService(this.reviewRepository, this.auditService);
        this.notificationService = new NotificationService(this.notificationRepository);

        // 8. Phase 7 AI & Advanced Integrations
        this.recommendationService = new RecommendationService(this.bookRepository, this.reportRepository, this.reviewRepository);
        this.mfsVerificationService = new MfsVerificationService(this.paymentRepository);
        this.notificationGatewayService = new NotificationGatewayService(this.notificationService);
        this.aiAssistantService = new AiAssistantService(
                this.catalogService,
                this.recommendationService,
                this.borrowingPolicyRepository,
                this.bookCopyRepository,
                this.shelfRepository
        );

        log.info("LibraryOS Application Context initialized successfully with Phases 1 through 7 services.");
    }

    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public PermissionRepository getPermissionRepository() {
        return permissionRepository;
    }

    public RoleRepository getRoleRepository() {
        return roleRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public AuditLogRepository getAuditLogRepository() {
        return auditLogRepository;
    }

    public AppSettingRepository getAppSettingRepository() {
        return appSettingRepository;
    }

    public DashboardRepository getDashboardRepository() {
        return dashboardRepository;
    }

    public PublisherRepository getPublisherRepository() {
        return publisherRepository;
    }

    public AuthorRepository getAuthorRepository() {
        return authorRepository;
    }

    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
    }

    public ShelfRepository getShelfRepository() {
        return shelfRepository;
    }

    public BookRepository getBookRepository() {
        return bookRepository;
    }

    public BookCopyRepository getBookCopyRepository() {
        return bookCopyRepository;
    }

    public MemberRepository getMemberRepository() {
        return memberRepository;
    }

    public BorrowingPolicyRepository getBorrowingPolicyRepository() {
        return borrowingPolicyRepository;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public SettingService getSettingService() {
        return settingService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public CatalogService getCatalogService() {
        return catalogService;
    }

    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public LoanRepository getLoanRepository() {
        return loanRepository;
    }

    public LoanRenewalRepository getLoanRenewalRepository() {
        return loanRenewalRepository;
    }

    public FineRepository getFineRepository() {
        return fineRepository;
    }

    public ReservationRepository getReservationRepository() {
        return reservationRepository;
    }

    public FineCalculationService getFineCalculationService() {
        return fineCalculationService;
    }

    public CirculationService getCirculationService() {
        return circulationService;
    }

    public FineAdjustmentRepository getFineAdjustmentRepository() {
        return fineAdjustmentRepository;
    }

    public PaymentRepository getPaymentRepository() {
        return paymentRepository;
    }

    public ReservationService getReservationService() {
        return reservationService;
    }

    public FineService getFineService() {
        return fineService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public ReportRepository getReportRepository() {
        return reportRepository;
    }

    public BarcodeService getBarcodeService() {
        return barcodeService;
    }

    public PdfExportService getPdfExportService() {
        return pdfExportService;
    }

    public ExcelExportService getExcelExportService() {
        return excelExportService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public BackupService getBackupService() {
        return backupService;
    }

    public SupplierRepository getSupplierRepository() {
        return supplierRepository;
    }

    public AcquisitionRepository getAcquisitionRepository() {
        return acquisitionRepository;
    }

    public DigitalBookRepository getDigitalBookRepository() {
        return digitalBookRepository;
    }

    public ReadingProgressRepository getReadingProgressRepository() {
        return readingProgressRepository;
    }

    public BookmarkRepository getBookmarkRepository() {
        return bookmarkRepository;
    }

    public ReviewRepository getReviewRepository() {
        return reviewRepository;
    }

    public NotificationRepository getNotificationRepository() {
        return notificationRepository;
    }

    public DigitalLibraryService getDigitalLibraryService() {
        return digitalLibraryService;
    }

    public AcquisitionService getAcquisitionService() {
        return acquisitionService;
    }

    public ReviewService getReviewService() {
        return reviewService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public RecommendationService getRecommendationService() {
        return recommendationService;
    }

    public MfsVerificationService getMfsVerificationService() {
        return mfsVerificationService;
    }

    public NotificationGatewayService getNotificationGatewayService() {
        return notificationGatewayService;
    }

    public AiAssistantService getAiAssistantService() {
        return aiAssistantService;
    }

    public static void shutdown() {
        if (instance != null) {
            log.info("Shutting down LibraryOS AppContext...");
            try {
                instance.executorService.shutdown();
                if (!instance.executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    instance.executorService.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                instance.executorService.shutdownNow();
            }
            DatabaseConfig.shutdown();
            instance = null;
        }
    }
}
