package com.ewb.payment.config;

import com.ewb.payment.entity.Account;
import com.ewb.payment.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(AccountRepository accountRepository) {
        return args -> {
            accountRepository.save(new Account(
                    "EWB-SAL-1001", "Maria Salary Account", "CUST-1001",
                    new BigDecimal("20000.00"), "PHP", "ACTIVE"));

            accountRepository.save(new Account(
                    "EWB-SAV-2001", "Maria Savings Account", "CUST-1001",
                    new BigDecimal("1000.00"), "PHP", "ACTIVE"));

            accountRepository.save(new Account(
                    "EWB-LOW-3001", "Maria Low Balance Account", "CUST-1001",
                    new BigDecimal("2000.00"), "PHP", "ACTIVE"));

            accountRepository.save(new Account(
                    "EWB-FRZ-4001", "Maria Frozen Account", "CUST-1001",
                    new BigDecimal("50000.00"), "PHP", "FROZEN"));
        };
    }
}
