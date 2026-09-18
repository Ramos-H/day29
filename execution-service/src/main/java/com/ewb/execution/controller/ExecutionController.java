package com.ewb.execution.controller;

import com.ewb.execution.entity.ExecutionRecord;
import com.ewb.execution.service.ExecutionProcessorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping
public class ExecutionController {

    private final ExecutionProcessorService service;

    public ExecutionController(ExecutionProcessorService service) {
        this.service = service;
    }

    @GetMapping("/standing-orders/{id}/executions")
    public ResponseEntity<List<ExecutionRecord>> getExecutionsForStandingOrder(@PathVariable Long id) {
        return ResponseEntity.ok(service.getExecutionsForStandingOrder(id));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionRecord>> getAllExecutions() {
        return ResponseEntity.ok(service.getAllExecutions());
    }

    @PostMapping("/executions/trigger")
    public ResponseEntity<List<ExecutionRecord>> triggerExecutions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.triggerExecutionRun(date));
    }
}
