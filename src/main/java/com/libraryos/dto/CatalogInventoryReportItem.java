package com.libraryos.dto;

public class CatalogInventoryReportItem {
    private Long bookId;
    private String titleEn;
    private String titleBn;
    private String isbn;
    private String authors;
    private String categories;
    private String publisher;
    private int totalCopies;
    private int availableCopies;
    private int issuedCopies;
    private int damagedOrLostCopies;

    public CatalogInventoryReportItem() {
    }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getTitleBn() { return titleBn; }
    public void setTitleBn(String titleBn) { this.titleBn = titleBn; }

    public String getDisplayName() {
        if (titleBn != null && !titleBn.isBlank()) {
            return titleEn + " (" + titleBn + ")";
        }
        return titleEn != null ? titleEn : "—";
    }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public int getIssuedCopies() { return issuedCopies; }
    public void setIssuedCopies(int issuedCopies) { this.issuedCopies = issuedCopies; }

    public int getDamagedOrLostCopies() { return damagedOrLostCopies; }
    public void setDamagedOrLostCopies(int damagedOrLostCopies) { this.damagedOrLostCopies = damagedOrLostCopies; }
}

