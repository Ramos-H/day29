package com.ewb.execution.service;

import com.ewb.execution.dto.*;
import com.ewb.execution.entity.ExecutionAttempt;
import com.ewb.execution.entity.ExecutionRecord;
import com.ewb.execution.entity.OutboxEvent;
import com.ewb.execution.repository.ExecutionAttemptRepository;
import com.ewb.execution.repository.ExecutionRecordRepository;
import com.ewb.execution.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExecutionProcessorService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionProcessorService.class);

    private final ExecutionRecordRepository executionRecordRepository;
    private final ExecutionAttemptRepository executionAttemptRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.services.standing-order-url:http://localhost:8081}")
    private String standingOrderServiceUrl;

    @Value("${app.services.payment-url:http://localhost:8083}")
    private String paymentServiceUrl;

    @Value("${app.services.notification-url:http://localhost:8084}")
    private String notificationServiceUrl;

    public ExecutionProcessorService(ExecutionRecordRepository executionRecordRepository,
                                     ExecutionAttemptRepository executionAttemptRepository,
                                     OutboxEventRepository outboxEventRepository,
                                     RestClient restClient,
                                     ObjectMapper objectMapper) {
        this.executionRecordRepository = executionRecordRepository;
        this.executionAttemptRepository = executionAttemptRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<ExecutionRecord> getExecutionsForStandingOrder(Long standingOrderId) {
        return executionRecordRepository.findByStandingOrderIdOrderByScheduledOccurrenceDesc(standingOrderId);
    }

    public List<ExecutionRecord> getAllExecutions() {
        return executionRecordRepository.findAll();
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledExecutionTask() {
        try {
            triggerExecutionRun(LocalDate.now());
        } catch (Exception e) {
            log.error("Scheduled execution task failed: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void scheduledOutboxPublisher() {
        try {
            publishPendingOutboxEvents();
        } catch (Exception e) {
            log.error("Outbox publisher failed: {}", e.getMessage());
        }
    }

    @Transactional
    public List<ExecutionRecord> triggerExecutionRun(LocalDate targetDate) {
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now();
        log.info("Starting execution run for date: {}", date);

        List<StandingOrderDto> dueOrders;
        try {
            dueOrders = restClient.get()
                    .uri(standingOrderServiceUrl + "/standing-orders/due?date=" + date)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StandingOrderDto>>() {});
        } catch (Exception e) {
            log.error("Failed to query due orders from Standing Order Service: {}", e.getMessage());
            return Collections.emptyList();
        }

        if (dueOrders == null || dueOrders.isEmpty()) {
            log.info("No due standing orders found for execution date: {}", date);
            return Collections.emptyList();
        }

        List<ExecutionRecord> processedRecords = new ArrayList<>();
        String workerId = "WORKER-" + UUID.randomUUID().toString().substring(0, 6);

        for (StandingOrderDto order : dueOrders) {
            LocalDate occurrence = order.getNextExecutionDate() != null ? order.getNextExecutionDate() : date;

            // Deduplication Check
            Optional<ExecutionRecord> existing = executionRecordRepository
                    .findByStandingOrderIdAndScheduledOccurrence(order.getId(), occurrence);
            if (existing.isPresent()) {
                log.info("Execution record already exists for order {} and occurrence {}. Skipping duplicate.",
                        order.getId(), occurrence);
                processedRecords.add(existing.get());
                continue;
            }

            // Create and claim execution record
            ExecutionRecord record = new ExecutionRecord();
            record.setStandingOrderId(order.getId());
            record.setScheduledOccurrence(occurrence);
            record.setInstructionVersion(order.getVersion());
            record.setStatus("PROCESSING");
            record.setWorkerClaimId(workerId);
            record.setLeaseExpiresAt(LocalDateTime.now().plusMinutes(5));
            record.setAttemptCount(0);
            record.setCreatedAt(LocalDateTime.now());

            String stableIdempotencyKey = String.format("SO-%d_%sT%s:00Z",
                    order.getId(),
                    occurrence,
                    order.getExecutionTime() != null ? order.getExecutionTime() : "09:00");
            record.setIdempotencyKey(stableIdempotencyKey);

            record = executionRecordRepository.save(record);
            processExecution(record, order);
            processedRecords.add(record);
        }

        return processedRecords;
    }

    private void processExecution(ExecutionRecord record, StandingOrderDto order) {
        // Revalidation check: if order is paused or cancelled, do not submit transfer
        if (!"ACTIVE".equalsIgnoreCase(order.getStatus())) {
            record.setStatus("SKIPPED_INACTIVE");
            record.setFailureReason("Standing order is currently " + order.getStatus());
            record.setCompletedAt(LocalDateTime.now());
            executionRecordRepository.save(record);
            return;
        }

        int attemptNumber = record.getAttemptCount() + 1;
        record.setAttemptCount(attemptNumber);

        TransferRequestDto transferReq = new TransferRequestDto(
                order.getSourceAccountId(),
                order.getDestinationAccountId(),
                order.getAmount(),
                order.getCurrency() != null ? order.getCurrency() : "PHP",
                record.getIdempotencyKey()
        );

        LocalDateTime attemptTime = LocalDateTime.now();

        try {
            TransferResponseDto response = restClient.post()
                    .uri(paymentServiceUrl + "/transfers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(transferReq)
                    .retrieve()
                    .body(TransferResponseDto.class);

            if (response != null && "SUCCESS".equals(response.getStatus())) {
                record.setStatus("SUCCESS");
                record.setPaymentReference(response.getPaymentReference());
                record.setCompletedAt(LocalDateTime.now());
                executionRecordRepository.save(record);

                executionAttemptRepository.save(new ExecutionAttempt(
                        record.getId(), attemptNumber, attemptTime, "SUCCESS", null, "Transfer completed successfully"));

                // Advance schedule in Standing Order Service
                advanceStandingOrder(order.getId());

                // Enqueue Outbox Event
                enqueueOutboxEvent(record, order, "PAYMENT_COMPLETED", response.getPaymentReference(),
                        "Transfer of ₱" + order.getAmount() + " completed successfully.");
            } else {
                handleBusinessFailure(record, order, attemptNumber, attemptTime,
                        response != null ? response.getStatus() : "FAILED",
                        response != null ? response.getMessage() : "Unknown transfer failure");
            }
        } catch (RestClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.warn("Payment service returned non-2xx status: {} body: {}", ex.getStatusCode(), errorBody);

            String status = "FAILED";
            String reason = ex.getMessage();
            try {
                TransferResponseDto errResp = objectMapper.readValue(errorBody, TransferResponseDto.class);
                if (errResp.getStatus() != null) status = errResp.getStatus();
                if (errResp.getMessage() != null) reason = errResp.getMessage();
            } catch (Exception ignored) {}

            handleBusinessFailure(record, order, attemptNumber, attemptTime, status, reason);
        } catch (Exception ex) {
            log.error("Payment submission timeout / network error: {}", ex.getMessage());
            record.setStatus("UNRESOLVED");
            record.setFailureReason("Timeout or communication failure: " + ex.getMessage());
            executionRecordRepository.save(record);

            executionAttemptRepository.save(new ExecutionAttempt(
                    record.getId(), attemptNumber, attemptTime, "TIMEOUT", "COMMUNICATION_ERROR", ex.getMessage()));
        }
    }

    private void handleBusinessFailure(ExecutionRecord record, StandingOrderDto order, int attemptNumber,
                                       LocalDateTime attemptTime, String status, String reason) {
        record.setStatus("FAILED");
        record.setFailureReason(reason);
        record.setCompletedAt(LocalDateTime.now());
        executionRecordRepository.save(record);

        executionAttemptRepository.save(new ExecutionAttempt(
                record.getId(), attemptNumber, attemptTime, "BUSINESS_REJECTION", status, reason));

        // Advance occurrence date so next month is not permanently blocked
        advanceStandingOrder(order.getId());

        // Enqueue Outbox Event
        enqueueOutboxEvent(record, order, "PAYMENT_FAILED", null,
                "Transfer of ₱" + order.getAmount() + " failed: " + reason);
    }

    private void advanceStandingOrder(Long orderId) {
        try {
            restClient.post()
                    .uri(standingOrderServiceUrl + "/standing-orders/" + orderId + "/advance")
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to advance standing order schedule {}: {}", orderId, e.getMessage());
        }
    }

    private void enqueueOutboxEvent(ExecutionRecord record, StandingOrderDto order, String eventType,
                                    String paymentRef, String details) {
        try {
            NotificationEventDto eventDto = new NotificationEventDto();
            eventDto.setEventId(UUID.randomUUID().toString());
            eventDto.setExecutionId(record.getId());
            eventDto.setStandingOrderId(order.getId());
            eventDto.setCustomerId(order.getCustomerId());
            eventDto.setEventType(eventType);
            eventDto.setPaymentReference(paymentRef);
            eventDto.setAmount(order.getAmount());
            eventDto.setStatus(record.getStatus());
            eventDto.setDetails(details);
            eventDto.setTimestamp(LocalDateTime.now());

            String payloadJson = objectMapper.writeValueAsString(eventDto);
            OutboxEvent outbox = new OutboxEvent(
                    eventDto.getEventId(), record.getId(), eventType, payloadJson, "PENDING", 0, LocalDateTime.now());
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event: {}", e.getMessage());
        }
    }

    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublicationStatus("PENDING");
        for (OutboxEvent event : pending) {
            try {
                restClient.post()
                        .uri(notificationServiceUrl + "/notifications/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(event.getPayload())
                        .retrieve()
                        .toBodilessEntity();

                event.setPublicationStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("Published outbox event {}", event.getEventId());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event {} (retry {}): {}", event.getEventId(), event.getRetryCount(), e.getMessage());
            }
        }
    }
}
