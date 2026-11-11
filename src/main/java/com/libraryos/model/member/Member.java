package com.libraryos.model.member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Member {
    private Long id;
    private String membershipNumber;
    private int memberTypeId;
    private String memberTypeName;
    private String fullNameEn;
    private String fullNameBn;
    private String institutionalId; // Student Roll or Staff ID
    private String department;
    private String program;
    private String semesterOrSession;
    private String phone;
    private String email;
    private String address;
    private String photoPath;
    private LocalDate membershipStartDate = LocalDate.now();
    private LocalDate membershipExpiryDate = LocalDate.now().plusYears(1);
    private MemberStatus status = MemberStatus.ACTIVE;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Derived active circulation counters
    private int activeLoansCount = 0;
    private BigDecimal unpaidFinesBdt = BigDecimal.ZERO;

    public Member() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public void setMembershipNumber(String membershipNumber) {
        this.membershipNumber = membershipNumber;
    }

    public int getMemberTypeId() {
        return memberTypeId;
    }

    public void setMemberTypeId(int memberTypeId) {
        this.memberTypeId = memberTypeId;
    }

    public String getMemberTypeName() {
        return memberTypeName;
    }

    public void setMemberTypeName(String memberTypeName) {
        this.memberTypeName = memberTypeName;
    }

    public String getFullNameEn() {
        return fullNameEn;
    }

    public void setFullNameEn(String fullNameEn) {
        this.fullNameEn = fullNameEn;
    }

    public String getFullNameBn() {
        return fullNameBn;
    }

    public void setFullNameBn(String fullNameBn) {
        this.fullNameBn = fullNameBn;
    }

    public String getInstitutionalId() {
        return institutionalId;
    }

    public void setInstitutionalId(String institutionalId) {
        this.institutionalId = institutionalId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getSemesterOrSession() {
        return semesterOrSession;
    }

    public void setSemesterOrSession(String semesterOrSession) {
        this.semesterOrSession = semesterOrSession;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public LocalDate getMembershipStartDate() {
        return membershipStartDate;
    }

    public void setMembershipStartDate(LocalDate membershipStartDate) {
        this.membershipStartDate = membershipStartDate;
    }

    public LocalDate getMembershipExpiryDate() {
        return membershipExpiryDate;
    }

    public void setMembershipExpiryDate(LocalDate membershipExpiryDate) {
        this.membershipExpiryDate = membershipExpiryDate;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status != null ? status : MemberStatus.ACTIVE;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    public void setActiveLoansCount(int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    public BigDecimal getUnpaidFinesBdt() {
        return unpaidFinesBdt;
    }

    public void setUnpaidFinesBdt(BigDecimal unpaidFinesBdt) {
        this.unpaidFinesBdt = unpaidFinesBdt != null ? unpaidFinesBdt : BigDecimal.ZERO;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE &&
                membershipExpiryDate != null &&
                !membershipExpiryDate.isBefore(LocalDate.now());
    }

    public String getDisplayName() {
        if (fullNameBn != null && !fullNameBn.isBlank()) {
            return fullNameEn + " (" + fullNameBn + ")";
        }
        return fullNameEn;
    }

    @Override
    public String toString() {
        return membershipNumber + " — " + getDisplayName();
    }
}

