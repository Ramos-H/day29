package com.ewb.execution.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_attempts")
public class ExecutionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long executionId;
    private Integer attemptNumber;
    private LocalDateTime timestamp;
    private String responseCategory; // SUCCESS, BUSINESS_REJECTION, TIMEOUT, SYSTEM_ERROR
    private String errorCode;
    private String errorMessage;

    public ExecutionAttempt() {}

    public ExecutionAttempt(Long executionId, Integer attemptNumber, LocalDateTime timestamp,
                            String responseCategory, String errorCode, String errorMessage) {
        this.executionId = executionId;
        this.attemptNumber = attemptNumber;
        this.timestamp = timestamp;
        this.responseCategory = responseCategory;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getResponseCategory() { return responseCategory; }
    public void setResponseCategory(String responseCategory) { this.responseCategory = responseCategory; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
