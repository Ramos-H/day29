package com.ewb.standingorder.service;

import com.ewb.standingorder.dto.AmendStandingOrderRequest;
import com.ewb.standingorder.dto.CreateStandingOrderRequest;
import com.ewb.standingorder.dto.StandingOrderResponse;
import com.ewb.standingorder.entity.StandingOrder;
import com.ewb.standingorder.repository.StandingOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StandingOrderService {

    private final StandingOrderRepository repository;

    public StandingOrderService(StandingOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StandingOrderResponse create(CreateStandingOrderRequest req, String customerId) {
        if (req.getSourceAccountId().equalsIgnoreCase(req.getDestinationAccountId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts cannot be identical");
        }

        if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be strictly greater than zero");
        }

        StandingOrder order = new StandingOrder();
        order.setCustomerId(customerId != null ? customerId : "CUST-1001");
        order.setSourceAccountId(req.getSourceAccountId());
        order.setDestinationAccountId(req.getDestinationAccountId());
        order.setAmount(req.getAmount());
        order.setCurrency(req.getCurrency() != null ? req.getCurrency() : "PHP");
        order.setFrequency(req.getFrequency() != null ? req.getFrequency() : "MONTHLY");
        order.setDayOfMonth(req.getDayOfMonth());
        order.setExecutionTime(req.getExecutionTime() != null ? req.getExecutionTime() : "09:00");
        order.setTimeZone(req.getTimeZone() != null ? req.getTimeZone() : "Asia/Manila");
        order.setStartDate(req.getStartDate());
        order.setEndDate(req.getEndDate());
        order.setStatus("ACTIVE");
        order.setVersion(1);

        LocalDate nextDate = calculateNextOccurrence(req.getStartDate(), req.getDayOfMonth());
        order.setNextExecutionDate(nextDate);

        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        StandingOrder saved = repository.save(order);
        return toResponse(saved);
    }

    public List<StandingOrderResponse> getByCustomer(String customerId) {
        String effectiveCustomer = (customerId != null && !customerId.isBlank()) ? customerId : "CUST-1001";
        return repository.findByCustomerId(effectiveCustomer).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public StandingOrderResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standing order not found: " + id));
    }

    @Transactional
    public StandingOrderResponse amend(Long id, AmendStandingOrderRequest req, String customerId) {
        StandingOrder order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standing order not found: " + id));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot amend a cancelled standing order");
        }

        if (req.getAmount() != null) {
            if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be strictly greater than zero");
            }
            order.setAmount(req.getAmount());
        }

        if (req.getDestinationAccountId() != null) {
            if (order.getSourceAccountId().equalsIgnoreCase(req.getDestinationAccountId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination accounts cannot be identical");
            }
            order.setDestinationAccountId(req.getDestinationAccountId());
        }

        if (req.getDayOfMonth() != null) {
            order.setDayOfMonth(req.getDayOfMonth());
            order.setNextExecutionDate(calculateNextOccurrence(LocalDate.now(), req.getDayOfMonth()));
        }

        if (req.getExecutionTime() != null) {
            order.setExecutionTime(req.getExecutionTime());
        }

        order.setVersion(order.getVersion() + 1);
        order.setUpdatedAt(LocalDateTime.now());

        return toResponse(repository.save(order));
    }

    @Transactional
    public StandingOrderResponse pause(Long id, String customerId) {
        StandingOrder order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standing order not found: " + id));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pause a cancelled standing order");
        }

        order.setStatus("PAUSED");
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(repository.save(order));
    }

    @Transactional
    public StandingOrderResponse resume(Long id, String customerId) {
        StandingOrder order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standing order not found: " + id));

        if (!"PAUSED".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only paused standing orders can be resumed");
        }

        order.setStatus("ACTIVE");
        order.setNextExecutionDate(calculateNextOccurrence(LocalDate.now(), order.getDayOfMonth()));
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(repository.save(order));
    }

    @Transactional
    public StandingOrderResponse cancel(Long id, String customerId) {
        StandingOrder order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Standing order not found: " + id));

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(repository.save(order));
    }

    public List<StandingOrderResponse> findDueOrders(LocalDate date) {
        LocalDate searchDate = date != null ? date : LocalDate.now();
        return repository.findByStatusAndNextExecutionDateLessThanEqual("ACTIVE", searchDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void advanceNextExecutionDate(Long id) {
        repository.findById(id).ifPresent(order -> {
            LocalDate currentNext = order.getNextExecutionDate() != null ? order.getNextExecutionDate() : LocalDate.now();
            LocalDate subsequent = calculateNextOccurrence(currentNext.plusDays(1), order.getDayOfMonth());
            order.setNextExecutionDate(subsequent);
            order.setUpdatedAt(LocalDateTime.now());
            repository.save(order);
        });
    }

    public LocalDate calculateNextOccurrence(LocalDate fromDate, int targetDay) {
        YearMonth ym = YearMonth.from(fromDate);
        int maxDayInMonth = ym.lengthOfMonth();
        int effectiveDay = Math.min(targetDay, maxDayInMonth);
        LocalDate candidate = ym.atDay(effectiveDay);

        if (!candidate.isBefore(fromDate)) {
            return candidate;
        }

        // Advance to next month
        YearMonth nextYm = ym.plusMonths(1);
        int maxDayNext = nextYm.lengthOfMonth();
        return nextYm.atDay(Math.min(targetDay, maxDayNext));
    }

    private StandingOrderResponse toResponse(StandingOrder order) {
        StandingOrderResponse resp = new StandingOrderResponse();
        resp.setId(order.getId());
        resp.setCustomerId(order.getCustomerId());
        resp.setSourceAccountId(order.getSourceAccountId());
        resp.setDestinationAccountId(order.getDestinationAccountId());
        resp.setAmount(order.getAmount());
        resp.setCurrency(order.getCurrency());
        resp.setFrequency(order.getFrequency());
        resp.setDayOfMonth(order.getDayOfMonth());
        resp.setExecutionTime(order.getExecutionTime());
        resp.setTimeZone(order.getTimeZone());
        resp.setStartDate(order.getStartDate());
        resp.setEndDate(order.getEndDate());
        resp.setStatus(order.getStatus());
        resp.setVersion(order.getVersion());
        resp.setNextExecutionDate(order.getNextExecutionDate());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());
        return resp;
    }
}
