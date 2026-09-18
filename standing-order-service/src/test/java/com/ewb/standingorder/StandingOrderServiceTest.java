package com.ewb.standingorder;

import com.ewb.standingorder.dto.AmendStandingOrderRequest;
import com.ewb.standingorder.dto.CreateStandingOrderRequest;
import com.ewb.standingorder.dto.StandingOrderResponse;
import com.ewb.standingorder.repository.StandingOrderRepository;
import com.ewb.standingorder.service.StandingOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StandingOrderServiceTest {

    @Autowired
    private StandingOrderService service;

    @Autowired
    private StandingOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testCreateValidStandingOrder() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-SAL-1001");
        req.setDestinationAccountId("EWB-SAV-2001");
        req.setAmount(new BigDecimal("5000.00"));
        req.setFrequency("MONTHLY");
        req.setDayOfMonth(25);
        req.setExecutionTime("09:00");
        req.setStartDate(LocalDate.of(2026, 10, 25));

        StandingOrderResponse resp = service.create(req, "CUST-1001");
        assertNotNull(resp.getId());
        assertEquals("ACTIVE", resp.getStatus());
        assertEquals(1, resp.getVersion());
        assertEquals(LocalDate.of(2026, 10, 25), resp.getNextExecutionDate());
    }

    @Test
    void testMonthEndClampingCalculation() {
        // Month end rule: Day 31 scheduled in April (30 days) resolves to April 30
        LocalDate aprilBase = LocalDate.of(2026, 4, 1);
        LocalDate occurrenceApril = service.calculateNextOccurrence(aprilBase, 31);
        assertEquals(LocalDate.of(2026, 4, 30), occurrenceApril, "April 31st must clamp to April 30th");

        // Month end rule: Day 31 scheduled in Feb 2026 (non-leap: 28 days) resolves to Feb 28
        LocalDate febBase = LocalDate.of(2026, 2, 1);
        LocalDate occurrenceFeb = service.calculateNextOccurrence(febBase, 31);
        assertEquals(LocalDate.of(2026, 2, 28), occurrenceFeb, "Feb 31st must clamp to Feb 28th");
    }

    @Test
    void testAmendStandingOrderBumpsVersion() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-SAL-1001");
        req.setDestinationAccountId("EWB-SAV-2001");
        req.setAmount(new BigDecimal("5000.00"));
        req.setDayOfMonth(25);
        req.setStartDate(LocalDate.of(2026, 10, 25));

        StandingOrderResponse created = service.create(req, "CUST-1001");

        AmendStandingOrderRequest amendReq = new AmendStandingOrderRequest();
        amendReq.setAmount(new BigDecimal("7500.00"));

        StandingOrderResponse amended = service.amend(created.getId(), amendReq, "CUST-1001");
        assertEquals(2, amended.getVersion());
        assertEquals(0, new BigDecimal("7500.00").compareTo(amended.getAmount()));
    }

    @Test
    void testPauseAndResumeLifecycle() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-SAL-1001");
        req.setDestinationAccountId("EWB-SAV-2001");
        req.setAmount(new BigDecimal("5000.00"));
        req.setDayOfMonth(25);
        req.setStartDate(LocalDate.of(2026, 10, 25));

        StandingOrderResponse created = service.create(req, "CUST-1001");

        StandingOrderResponse paused = service.pause(created.getId(), "CUST-1001");
        assertEquals("PAUSED", paused.getStatus());

        StandingOrderResponse resumed = service.resume(created.getId(), "CUST-1001");
        assertEquals("ACTIVE", resumed.getStatus());
    }

    @Test
    void testIdenticalAccountsRejection() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-SAL-1001");
        req.setDestinationAccountId("EWB-SAL-1001");
        req.setAmount(new BigDecimal("5000.00"));
        req.setDayOfMonth(25);
        req.setStartDate(LocalDate.of(2026, 10, 25));

        assertThrows(ResponseStatusException.class, () -> service.create(req, "CUST-1001"));
    }
}
