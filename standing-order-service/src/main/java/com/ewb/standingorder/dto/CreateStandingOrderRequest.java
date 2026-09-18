package com.ewb.standingorder.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateStandingOrderRequest {

    @NotBlank(message = "Source account ID is required")
    private String sourceAccountId;

    @NotBlank(message = "Destination account ID is required")
    private String destinationAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
    private BigDecimal amount;

    private String currency = "PHP";

    private String frequency = "MONTHLY";

    @NotNull(message = "Day of month is required")
    @Min(value = 1, message = "Day of month must be between 1 and 31")
    @Max(value = 31, message = "Day of month must be between 1 and 31")
    private Integer dayOfMonth;

    private String executionTime = "09:00";
    private String timeZone = "Asia/Manila";

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    public CreateStandingOrderRequest() {}

    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public String getExecutionTime() { return executionTime; }
    public void setExecutionTime(String executionTime) { this.executionTime = executionTime; }

    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
