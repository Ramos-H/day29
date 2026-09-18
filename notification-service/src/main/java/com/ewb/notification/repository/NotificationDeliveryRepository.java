package com.ewb.notification.repository;

import com.ewb.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    Optional<NotificationDelivery> findByEventId(String eventId);
    List<NotificationDelivery> findByCustomerIdOrderByDeliveredAtDesc(String customerId);
}
