package com.ewb.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private String accountId;
    private String accountName;
    private String customerId;
    private BigDecimal balance;
    private String currency;
    private String status; // ACTIVE, FROZEN

    public Account() {}

    public Account(String accountId, String accountName, String customerId, BigDecimal balance, String currency, String status) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.customerId = customerId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
