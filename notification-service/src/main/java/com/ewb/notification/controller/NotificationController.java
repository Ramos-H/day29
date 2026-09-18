package com.ewb.notification.controller;

import com.ewb.notification.dto.NotificationEvent;
import com.ewb.notification.entity.NotificationDelivery;
import com.ewb.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/events")
    public ResponseEntity<NotificationDelivery> receiveEvent(@RequestBody NotificationEvent event) {
        return ResponseEntity.ok(service.processEvent(event));
    }

    @GetMapping
    public ResponseEntity<List<NotificationDelivery>> list(
            @RequestParam(required = false) String customerId) {
        return ResponseEntity.ok(service.getNotifications(customerId));
    }
}
