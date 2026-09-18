package com.ewb.payment.controller;

import com.ewb.payment.dto.TransferRequest;
import com.ewb.payment.dto.TransferResponse;
import com.ewb.payment.entity.Account;
import com.ewb.payment.entity.PaymentRecord;
import com.ewb.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = paymentService.executeTransfer(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }

    @GetMapping("/transfers/by-reference/{reference}")
    public ResponseEntity<PaymentRecord> getPaymentByReference(@PathVariable String reference) {
        return paymentService.getPaymentByReference(reference)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment reference not found: " + reference));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        return paymentService.getAccount(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id));
    }
}
