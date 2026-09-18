package com.ewb.standingorder.dto;

import java.math.BigDecimal;

public class AmendStandingOrderRequest {

    private BigDecimal amount;
    private Integer dayOfMonth;
    private String executionTime;
    private String destinationAccountId;

    public AmendStandingOrderRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public String getExecutionTime() { return executionTime; }
    public void setExecutionTime(String executionTime) { this.executionTime = executionTime; }

    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }
}
