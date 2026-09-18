package com.ewb.execution.repository;

import com.ewb.execution.entity.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {
    List<ExecutionRecord> findByStandingOrderIdOrderByScheduledOccurrenceDesc(Long standingOrderId);
    Optional<ExecutionRecord> findByStandingOrderIdAndScheduledOccurrence(Long standingOrderId, LocalDate scheduledOccurrence);
    List<ExecutionRecord> findByStatus(String status);
}
