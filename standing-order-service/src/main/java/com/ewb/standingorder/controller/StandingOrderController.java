package com.ewb.standingorder.controller;

import com.ewb.standingorder.dto.AmendStandingOrderRequest;
import com.ewb.standingorder.dto.CreateStandingOrderRequest;
import com.ewb.standingorder.dto.StandingOrderResponse;
import com.ewb.standingorder.service.StandingOrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/standing-orders")
public class StandingOrderController {

    private final StandingOrderService service;

    public StandingOrderController(StandingOrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StandingOrderResponse> create(
            @Valid @RequestBody CreateStandingOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-1001") String customerId) {
        StandingOrderResponse response = service.create(request, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StandingOrderResponse>> list(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-1001") String customerId) {
        return ResponseEntity.ok(service.getByCustomer(customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandingOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StandingOrderResponse> amend(
            @PathVariable Long id,
            @RequestBody AmendStandingOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-1001") String customerId) {
        return ResponseEntity.ok(service.amend(id, request, customerId));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<StandingOrderResponse> pause(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-1001") String customerId) {
        return ResponseEntity.ok(service.pause(id, customerId));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<StandingOrderResponse> resume(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-1001") String customerId) {
        return ResponseEntity.ok(service.resume(id, customerId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<StandingOrderResponse> cancel(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-1001") String customerId) {
        return ResponseEntity.ok(service.cancel(id, customerId));
    }

    @GetMapping("/due")
    public ResponseEntity<List<StandingOrderResponse>> getDueOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.findDueOrders(date));
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<Void> advance(@PathVariable Long id) {
        service.advanceNextExecutionDate(id);
        return ResponseEntity.ok().build();
    }
}
