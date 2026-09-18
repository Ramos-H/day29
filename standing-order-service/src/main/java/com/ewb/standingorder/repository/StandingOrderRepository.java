package com.ewb.standingorder.repository;

import com.ewb.standingorder.entity.StandingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StandingOrderRepository extends JpaRepository<StandingOrder, Long> {
    List<StandingOrder> findByCustomerId(String customerId);
    List<StandingOrder> findByStatusAndNextExecutionDateLessThanEqual(String status, LocalDate date);
}
