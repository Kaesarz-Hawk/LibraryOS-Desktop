package com.libraryos.repository;

import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentAllocation;
import com.libraryos.model.finance.PaymentMethod;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment create(Payment payment, Connection conn);
    void update(Payment payment, Connection conn);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByIdForUpdate(Long id, Connection conn);
    Optional<Payment> findByReceiptNumber(String receiptNumber);
    Optional<Payment> findByExternalReference(String externalReference);
    List<Payment> findByMemberId(Long memberId);
    List<Payment> search(String query, PaymentMethod method, Boolean reversedOnly);
    void insertAllocation(PaymentAllocation allocation, Connection conn);
    List<PaymentAllocation> findAllocationsByPaymentId(Long paymentId);
    List<PaymentAllocation> findAllocationsByPaymentId(Long paymentId, Connection conn);
}

