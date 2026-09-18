package com.ewb.payment.service;

import com.ewb.payment.dto.TransferRequest;
import com.ewb.payment.dto.TransferResponse;
import com.ewb.payment.entity.Account;
import com.ewb.payment.entity.IdempotencyRecord;
import com.ewb.payment.entity.LedgerEntry;
import com.ewb.payment.entity.PaymentRecord;
import com.ewb.payment.repository.AccountRepository;
import com.ewb.payment.repository.IdempotencyRecordRepository;
import com.ewb.payment.repository.LedgerEntryRepository;
import com.ewb.payment.repository.PaymentRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public PaymentService(AccountRepository accountRepository,
                          LedgerEntryRepository ledgerEntryRepository,
                          PaymentRecordRepository paymentRecordRepository,
                          IdempotencyRecordRepository idempotencyRecordRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Transactional
    public TransferResponse executeTransfer(TransferRequest request) {
        // Validation: Source and destination cannot be identical
        if (request.getSourceAccountId().equalsIgnoreCase(request.getDestinationAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts cannot be identical");
        }

        // Validation: Amount must be positive
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be strictly greater than zero");
        }

        String fingerprint = String.format("%s|%s|%s|%s",
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount().stripTrailingZeros().toPlainString(),
                request.getCurrency());

        // Idempotency check
        Optional<IdempotencyRecord> existingIdempotency = idempotencyRecordRepository.findById(request.getIdempotencyKey());
        if (existingIdempotency.isPresent()) {
            IdempotencyRecord record = existingIdempotency.get();
            if (!record.getRequestFingerprint().equals(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key reuse detected with mismatching transfer parameters");
            }
            // Return cached result
            PaymentRecord existingPayment = paymentRecordRepository.findById(record.getPaymentReference())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment record not found for existing idempotency key"));

            return new TransferResponse(
                    existingPayment.getPaymentReference(),
                    existingPayment.getIdempotencyKey(),
                    existingPayment.getSourceAccountId(),
                    existingPayment.getDestinationAccountId(),
                    existingPayment.getAmount(),
                    existingPayment.getCurrency(),
                    existingPayment.getStatus(),
                    existingPayment.getStatus().equals("SUCCESS") ? "Idempotent duplicate transfer returned successfully" : existingPayment.getFailureReason(),
                    existingPayment.getCreatedAt()
            );
        }

        String paymentRef = request.getPaymentReference() != null && !request.getPaymentReference().isBlank()
                ? request.getPaymentReference()
                : "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Optional<Account> sourceOpt = accountRepository.findById(request.getSourceAccountId());
        Optional<Account> destOpt = accountRepository.findById(request.getDestinationAccountId());

        if (sourceOpt.isEmpty() || destOpt.isEmpty()) {
            return recordFailure(paymentRef, request.getIdempotencyKey(), fingerprint,
                    request.getSourceAccountId(), request.getDestinationAccountId(),
                    request.getAmount(), request.getCurrency(),
                    "FAILED_INVALID_ACCOUNT", "Source or destination account does not exist");
        }

        Account source = sourceOpt.get();
        Account dest = destOpt.get();

        // Check if account is frozen
        if ("FROZEN".equalsIgnoreCase(source.getStatus())) {
            return recordFailure(paymentRef, request.getIdempotencyKey(), fingerprint,
                    request.getSourceAccountId(), request.getDestinationAccountId(),
                    request.getAmount(), request.getCurrency(),
                    "FAILED_ACCOUNT_FROZEN", "Source account is frozen. Transactions are prohibited.");
        }

        // Check sufficient funds
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            return recordFailure(paymentRef, request.getIdempotencyKey(), fingerprint,
                    request.getSourceAccountId(), request.getDestinationAccountId(),
                    request.getAmount(), request.getCurrency(),
                    "FAILED_INSUFFICIENT_FUNDS", "Insufficient available funds in source account");
        }

        // Atomic double-entry update
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        dest.setBalance(dest.getBalance().add(request.getAmount()));
        accountRepository.save(source);
        accountRepository.save(dest);

        LocalDateTime now = LocalDateTime.now();

        // Ledger entries
        ledgerEntryRepository.save(new LedgerEntry(paymentRef, source.getAccountId(), "DEBIT", request.getAmount(), now));
        ledgerEntryRepository.save(new LedgerEntry(paymentRef, dest.getAccountId(), "CREDIT", request.getAmount(), now));

        // Payment record
        PaymentRecord paymentRecord = new PaymentRecord(
                paymentRef, request.getIdempotencyKey(), source.getAccountId(), dest.getAccountId(),
                request.getAmount(), request.getCurrency(), "SUCCESS", null, now);
        paymentRecordRepository.save(paymentRecord);

        // Idempotency record
        idempotencyRecordRepository.save(new IdempotencyRecord(
                request.getIdempotencyKey(), paymentRef, fingerprint, "SUCCESS", "SUCCESS", now));

        return new TransferResponse(paymentRef, request.getIdempotencyKey(), source.getAccountId(), dest.getAccountId(),
                request.getAmount(), request.getCurrency(), "SUCCESS", "Transfer executed atomically", now);
    }

    private TransferResponse recordFailure(String paymentRef, String idempotencyKey, String fingerprint,
                                           String sourceId, String destId, BigDecimal amount, String currency,
                                           String status, String reason) {
        LocalDateTime now = LocalDateTime.now();
        PaymentRecord record = new PaymentRecord(paymentRef, idempotencyKey, sourceId, destId, amount, currency, status, reason, now);
        paymentRecordRepository.save(record);

        idempotencyRecordRepository.save(new IdempotencyRecord(idempotencyKey, paymentRef, fingerprint, status, reason, now));

        return new TransferResponse(paymentRef, idempotencyKey, sourceId, destId, amount, currency, status, reason, now);
    }

    public Optional<PaymentRecord> getPaymentByReference(String reference) {
        return paymentRecordRepository.findById(reference)
                .or(() -> paymentRecordRepository.findByIdempotencyKey(reference));
    }

    public Optional<Account> getAccount(String accountId) {
        return accountRepository.findById(accountId);
    }
}
