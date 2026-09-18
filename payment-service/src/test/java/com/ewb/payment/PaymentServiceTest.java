package com.ewb.payment;

import com.ewb.payment.dto.TransferRequest;
import com.ewb.payment.dto.TransferResponse;
import com.ewb.payment.entity.Account;
import com.ewb.payment.repository.AccountRepository;
import com.ewb.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        // Reset balances
        accountRepository.save(new Account("EWB-SAL-1001", "Maria Salary Account", "CUST-1001",
                new BigDecimal("20000.00"), "PHP", "ACTIVE"));
        accountRepository.save(new Account("EWB-SAV-2001", "Maria Savings Account", "CUST-1001",
                new BigDecimal("1000.00"), "PHP", "ACTIVE"));
        accountRepository.save(new Account("EWB-LOW-3001", "Maria Low Balance Account", "CUST-1001",
                new BigDecimal("2000.00"), "PHP", "ACTIVE"));
        accountRepository.save(new Account("EWB-FRZ-4001", "Maria Frozen Account", "CUST-1001",
                new BigDecimal("50000.00"), "PHP", "FROZEN"));
    }

    @Test
    void testSuccessfulAtomicTransfer() {
        TransferRequest req = new TransferRequest(
                "EWB-SAL-1001", "EWB-SAV-2001",
                new BigDecimal("5000.00"), "PHP", "TEST-KEY-001");

        TransferResponse resp = paymentService.executeTransfer(req);
        assertEquals("SUCCESS", resp.getStatus());
        assertNotNull(resp.getPaymentReference());

        Account source = accountRepository.findById("EWB-SAL-1001").orElseThrow();
        Account dest = accountRepository.findById("EWB-SAV-2001").orElseThrow();

        assertEquals(0, new BigDecimal("15000.00").compareTo(source.getBalance()));
        assertEquals(0, new BigDecimal("6000.00").compareTo(dest.getBalance()));
    }

    @Test
    void testDuplicateRequestReturnsIdempotentResultWithoutDoubleDebit() {
        TransferRequest req = new TransferRequest(
                "EWB-SAL-1001", "EWB-SAV-2001",
                new BigDecimal("5000.00"), "PHP", "TEST-IDEM-002");

        TransferResponse resp1 = paymentService.executeTransfer(req);
        assertEquals("SUCCESS", resp1.getStatus());

        // Second submission with exact same key
        TransferResponse resp2 = paymentService.executeTransfer(req);
        assertEquals("SUCCESS", resp2.getStatus());
        assertEquals(resp1.getPaymentReference(), resp2.getPaymentReference());

        Account source = accountRepository.findById("EWB-SAL-1001").orElseThrow();
        assertEquals(0, new BigDecimal("15000.00").compareTo(source.getBalance()),
                "Balance must be debited only once");
    }

    @Test
    void testInsufficientFundsRejection() {
        TransferRequest req = new TransferRequest(
                "EWB-LOW-3001", "EWB-SAV-2001",
                new BigDecimal("5000.00"), "PHP", "TEST-INSUF-003");

        TransferResponse resp = paymentService.executeTransfer(req);
        assertEquals("FAILED_INSUFFICIENT_FUNDS", resp.getStatus());

        Account source = accountRepository.findById("EWB-LOW-3001").orElseThrow();
        assertEquals(0, new BigDecimal("2000.00").compareTo(source.getBalance()),
                "Source balance must remain unchanged");
    }

    @Test
    void testFrozenAccountRejection() {
        TransferRequest req = new TransferRequest(
                "EWB-FRZ-4001", "EWB-SAV-2001",
                new BigDecimal("5000.00"), "PHP", "TEST-FRZ-004");

        TransferResponse resp = paymentService.executeTransfer(req);
        assertEquals("FAILED_ACCOUNT_FROZEN", resp.getStatus());

        Account source = accountRepository.findById("EWB-FRZ-4001").orElseThrow();
        assertEquals(0, new BigDecimal("50000.00").compareTo(source.getBalance()),
                "Frozen balance must remain unchanged");
    }

    @Test
    void testIdenticalAccountsRejection() {
        TransferRequest req = new TransferRequest(
                "EWB-SAL-1001", "EWB-SAL-1001",
                new BigDecimal("5000.00"), "PHP", "TEST-IDENT-005");

        assertThrows(ResponseStatusException.class, () -> paymentService.executeTransfer(req));
    }
}
