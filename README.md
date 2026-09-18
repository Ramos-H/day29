# EWB Standing Order Processor

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Build](https://img.shields.io/badge/Build-Maven%203.9-red.svg)](https://maven.apache.org/)

An enterprise-grade, distributed microservices solution designed for **East West Bank (EWB)** to automate recurring customer transfers (standing orders), guarantee atomic double-entry financial ledgering, eliminate duplicate payments through stable idempotency, and provide resilient customer notifications.

---

## 🌟 Architecture Overview

The system is architected around autonomous, decoupled microservices adhering strictly to the **Database-per-Service** design pattern:

```text
                               +------------------------------------------+
                               |     Client / Front-End / Postman         |
                               +--------------------+---------------------+
                                                    | HTTP / REST (8080)
                                                    v
+------------------------+           +--------------+---------------+
| Netflix Eureka Registry|<----------+      Spring Cloud Gateway    |
|       (Port 8761)      |           |          (Port 8080)         |
+-----------+------------+           +--------------+---------------+
            ^                                       |
            | Dynamic Registration                  | Routing by Path
    +-------+--------------------+------------------+-------------------+
    |                            |                                      |
    v (Port 8081)                v (Port 8082)                          v (Port 8083)
+---+--------------------+   +---+--------------------+             +---+--------------------+
| Standing Order Service |   |   Execution Service    |             |    Payment Service     |
|  - Lifecycle (CRUD)    |   |  - Scheduler Poller    |             |  - Mock Core Banking   |
|  - Month-End Rules     |   |  - Claim Lease Lock    |             |  - Atomic Transfers    |
|  - Version Bumping     |   |  - Stable Idempotency  |             |  - Double-Entry Ledger |
+-----------+------------+   +---+--------+-----------+             +-----------+------------+
            |                    |        |                                     |
    [StandingOrder DB]           |   [Execution DB]                        [Payment DB]
                                 |        |
                                 |        | Transactional Outbox (POST /notifications/events)
                                 |        v
                                 |   +----+-------------------+
     Discovers Due Orders        |   |  Notification Service  |
     GET /standing-orders/due    |   |      (Port 8084)       |
     ----------------------------+   |  - Deduplicated Alert  |
                                     |  - SMS / Email Mock    |
                                     +----+-------------------+
                                          |
                                    [Delivery DB]

+--------------------------------------------------------------------------------------------+
| Spring Cloud Config Server (Port 8888) - Native externalized configs from classpath/repo   |
+--------------------------------------------------------------------------------------------+
```

---

## 👥 Engineering Team & Roles

| Developer | Role & Title | Owned Module | Key Responsibilities |
| :--- | :--- | :--- | :--- |
| **Felix Bueno IV** | Team Leader & Lead Domain Architect | `standing-order-service` | Standing order lifecycle state machine (`ACTIVE`, `PAUSED`, `CANCELLED`), version incrementing, month-end date clamping, and customer validation. |
| **Lanz Peredeon Lozaldo** | Core Banking & Data Persistence Engineer | `payment-service` | Mock core banking, double-entry ledger journals (`DEBIT`/`CREDIT`), atomic balance updates (`@Transactional`), and idempotency store. |
| **Joaquin Kester Abelardo** | Execution & Resilience Engineer | `execution-service` | Scheduled discovery poller, composite unique constraint deduplication, worker claim leases, payment client dispatch, and Transactional Outbox. |
| **Hans Simon Ramos** | Infrastructure & DevOps QA Engineer | `eureka-server`, `config-server`, `api-gateway`, `notification-service` | Eureka discovery registry, centralized config server, API gateway routing, notification deduplication, Docker Compose orchestration, and testing runbook. |

---

## 📂 Project Structure

```text
day29/
├── pom.xml                                   # Multi-module reactor build configuration
├── docker-compose.yml                        # Docker multi-container manifest
├── activity-sprint-guide.md                  # Comprehensive sprint documentation
├── README.md                                 # Project documentation
│
├── eureka-server/                            # Netflix Eureka Registry (Port 8761)
│   ├── Dockerfile
│   └── src/main/java/com/ewb/eureka/EurekaServerApplication.java
│
├── config-server/                            # Centralized Config Server (Port 8888)
│   ├── Dockerfile
│   └── src/main/resources/config-repo/       # Externalized service properties
│
├── api-gateway/                              # Spring Cloud Gateway (Port 8080)
│   ├── Dockerfile
│   └── src/main/resources/application.yml    # Route mapping: /standing-orders/**, /transfers/**, etc.
│
├── standing-order-service/                   # Standing Order Domain Service (Port 8081)
│   ├── Dockerfile
│   ├── src/main/java/com/ewb/standingorder/
│   └── src/test/java/com/ewb/standingorder/  # Unit tests (Month-end clamping, versioning, pause/resume)
│
├── payment-service/                          # Mock Core Banking Service (Port 8083)
│   ├── Dockerfile
│   ├── src/main/java/com/ewb/payment/
│   └── src/test/java/com/ewb/payment/        # Unit tests (Atomic transfers, idempotency, frozen accounts)
│
├── execution-service/                        # Execution Orchestrator & Poller (Port 8082)
│   ├── Dockerfile
│   └── src/main/java/com/ewb/execution/      # Deduplication, claim leases, outbox publisher
│
└── notification-service/                     # Customer Notification Service (Port 8084)
    ├── Dockerfile
    └── src/main/java/com/ewb/notification/   # Event consumer and deduplicator by eventId
```

---

## ⚙️ Technology Stack & Versions

* **Runtime**: Java 21 LTS (Oracle / OpenJDK)
* **Framework**: Spring Boot 4.1.1 (Locked primary release)
* **Cloud BOM**: Spring Cloud 2025.1.3
* **Service Registry**: Spring Cloud Netflix Eureka 5.0.2
* **Config Management**: Spring Cloud Config 5.0.5
* **API Gateway**: Spring Cloud Gateway Server MVC 5.0.3
* **Persistence**: Spring Data JPA / Hibernate Core 7.x
* **Database**: In-Memory H2 2.3.x (Isolated instance per service)
* **Build System**: Apache Maven 3.9.16
* **Containerization**: Docker 29.7.2 / Docker Compose

---

## 🚀 Getting Started

### Prerequisites
* **Java JDK 21+** installed (`java -version`)
* **Apache Maven 3.9+** installed (`mvn -version`)
* *(Optional)* **Docker Desktop** for multi-container deployment

### 1. Build and Test
Compile and run unit tests across all 7 modules:
```powershell
# In the project root (day29)
mvn clean test
```

### 2. Package Artifacts
Package executable standalone JARs for all services:
```powershell
mvn clean package -DskipTests
```

---

## 🏃 Running the System

### Option A: Local Execution (PowerShell)
Start services in separate terminal windows in the following dependency order:

1. **Eureka Discovery Server**:
   ```powershell
   java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar
   ```
2. **Config Server**:
   ```powershell
   java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar
   ```
3. **Core Domain Microservices**:
   ```powershell
   java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar
   java -jar standing-order-service/target/standing-order-service-1.0.0-SNAPSHOT.jar
   java -jar execution-service/target/execution-service-1.0.0-SNAPSHOT.jar
   java -jar notification-service/target/notification-service-1.0.0-SNAPSHOT.jar
   ```
4. **API Gateway**:
   ```powershell
   java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
   ```

### Option B: Docker Compose
Build and launch all 7 services within isolated containers connected via `ewb-net`:
```powershell
docker compose up --build
```
To stop and remove containers:
```powershell
docker compose down
```

---

## 🧪 Verification Runbook & Scenarios

The API Gateway routes all requests through `http://localhost:8080`.

### 1. Seed Accounts (Pre-Populated in Mock Core Banking)
* `EWB-SAL-1001`: Maria's Salary Account (Starting Balance: **₱20,000.00**, ACTIVE)
* `EWB-SAV-2001`: Maria's Savings Account (Starting Balance: **₱1,000.00**, ACTIVE)
* `EWB-LOW-3001`: Low Balance Account (Starting Balance: **₱2,000.00**, ACTIVE)
* `EWB-FRZ-4001`: Frozen Account (Starting Balance: **₱50,000.00**, FROZEN)

---

### 2. Scenario Walkthrough

#### Scenario 1: Successful Transfer (₱20,000 -> ₱15,000 & ₱1,000 -> ₱6,000)
1. **Create Maria's Standing Order**:
   ```powershell
   curl.exe -X POST http://localhost:8080/standing-orders `
     -H "Content-Type: application/json" `
     -H "X-User-Id: CUST-1001" `
     -d '{
       "sourceAccountId": "EWB-SAL-1001",
       "destinationAccountId": "EWB-SAV-2001",
       "amount": 5000.00,
       "currency": "PHP",
       "frequency": "MONTHLY",
       "dayOfMonth": 25,
       "startDate": "2026-10-25"
     }'
   ```
2. **Trigger Execution for Date `2026-10-25`**:
   ```powershell
   curl.exe -X POST "http://localhost:8080/executions/trigger?date=2026-10-25"
   ```
3. **Verify Updated Balances**:
   ```powershell
   curl.exe -s http://localhost:8080/accounts/EWB-SAL-1001
   # Returns: {"accountId":"EWB-SAL-1001","balance":15000.00,...}
   curl.exe -s http://localhost:8080/accounts/EWB-SAV-2001
   # Returns: {"accountId":"EWB-SAV-2001","balance":6000.00,...}
   ```

#### Scenario 2: Insufficient Funds Guard
```powershell
curl.exe -X POST http://localhost:8080/transfers `
  -H "Content-Type: application/json" `
  -d '{
    "sourceAccountId": "EWB-LOW-3001",
    "destinationAccountId": "EWB-SAV-2001",
    "amount": 5000.00,
    "idempotencyKey": "TEST-INSUF-KEY"
  }'
```
*Expected: HTTP 422 with status `FAILED_INSUFFICIENT_FUNDS`. Balances remain unchanged.*

#### Scenario 3: Frozen Account Rejection
```powershell
curl.exe -X POST http://localhost:8080/transfers `
  -H "Content-Type: application/json" `
  -d '{
    "sourceAccountId": "EWB-FRZ-4001",
    "destinationAccountId": "EWB-SAV-2001",
    "amount": 5000.00,
    "idempotencyKey": "TEST-FRZ-KEY"
  }'
```
*Expected: HTTP 422 with status `FAILED_ACCOUNT_FROZEN`. Zero ledger movements recorded.*

#### Scenario 4: Stable Idempotency (Duplicate Prevention)
Submitting the identical transfer request twice with the same `idempotencyKey`:
* First call performs atomic balance deduction and returns `HTTP 200 OK`.
* Second call returns the exact same payment reference without modifying account balances.

#### Scenario 5: Month-End Scheduling (Day 31 on Shorter Months)
Creating a monthly order for day 31 starting on `2026-04-01`:
* The system automatically clamps `nextExecutionDate` to `2026-04-30` (or `2026-02-28` for February).

#### Scenario 6: Pause and Resume Lifecycle
```powershell
# Pause instruction
curl.exe -X POST http://localhost:8080/standing-orders/1/pause

# Resume instruction
curl.exe -X POST http://localhost:8080/standing-orders/1/resume
```

#### Scenario 7: Outbox Publishing & Notification Deduplication
```powershell
curl.exe -s http://localhost:8080/notifications
```
*Returns customer delivery logs dispatched via the Execution Service's Transactional Outbox.*

---
## 📄 License
Internal training and technical evaluation material for East West Bank (EWB). All rights reserved.
