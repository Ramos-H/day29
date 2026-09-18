package com.ewb.payment.repository;

import com.ewb.payment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByPaymentReference(String paymentReference);
    List<LedgerEntry> findByAccountId(String accountId);
}
