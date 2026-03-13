# SemiTrack – Distributed Semiconductor Test Data Processing System

A Java 17 + Spring Boot backend that simulates a real semiconductor wafer/chip testing environment. Multiple concurrent **Test Station** services generate simulated wafer test data (pass/fail results, parametric values, die coordinates). A central **Aggregator** service collects, validates, and reports this data in real time via a REST API and a live dashboard.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        SemiTrack System                      │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Parametric   │  │ Parametric   │  │   Binary     │      │
│  │ Station ST-01│  │ Station ST-02│  │ Station ST-03│      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │ BlockingQueue (IPC)                │              │
│         └──────────────────┴────────────────┘              │
│                            │                                │
│                  ┌─────────▼──────────┐                    │
│                  │  AggregatorService │                    │
│                  │  (Consumer Thread) │                    │
│                  └─────────┬──────────┘                    │
│                            │                                │
│                  ┌─────────▼──────────┐                    │
│                  │  AggregationStore  │                    │
│                  │  (ReadWriteLock)   │                    │
│                  └──┬──────┬──────┬───┘                    │
│                     │      │      │                         │
│            ┌────────┘  ┌───┘  ┌───┘                        │
│            ▼           ▼      ▼                             │
│       REST API       RMI    gRPC                            │
│       (Spring)    Server   Server                           │
│            │                                                │
│       ┌────▼──────┐                                         │
│       │ Dashboard  │ (Thymeleaf + Chart.js)                 │
│       └────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Feature Breakdown

### 1. Core Java Collections

| Collection | Usage |
|---|---|
| `TreeMap<String, TreeMap<WaferDie, WaferTestResult>>` | Outer wafer map (sorted by wafer ID) with inner die map (sorted by coordinate) |
| `TreeSet<WaferDie>` | Ensures unique, ordered die coordinates when stations generate test data |
| `ArrayList` | Batch buffers in the consumer thread and result snapshots returned by queries |
| `HashMap<String, Double>` | Parametric measurements per die (voltage, threshold, leakage current) |
| `LinkedBlockingQueue` | IPC buffer between producer stations and the consumer aggregator |

**Tree traversal:** `store.resultsForWafer(id)` returns dies in row-major order (sort by X then Y) because `WaferDie` implements `Comparable` and is used as the key in a `TreeMap`. This means iterating the map is equivalent to a deterministic in-order traversal of the wafer grid.

---

### 2. Object-Oriented Design

```
TestStation (abstract)
├── ParametricTestStation   // measures voltage + threshold
└── BinaryTestStation       // go/no-go burn-in test

Validator (interface)
├── CoordinateValidator     // checks die bounds (0..24 × 0..24)
└── RangeValidator          // checks parametric value range

ReportGenerator (interface)
├── ConsoleReportGenerator  // human-readable stdout summary
└── JsonReportGenerator     // JSON serialisation via Jackson
```

- **Encapsulation:** `stationId` and `aggregator` are private in `TestStation`; `store` is private in `AggregatorService`.
- **Inheritance:** Both station types extend `TestStation` and override only `generateResults()`.
- **Polymorphism:** The simulation runner holds `List<TestStation>` and calls `station.runTests()` — the correct subtype executes at runtime.

---

### 3. Multi-Threaded Programming

- **`ExecutorService`** (`Executors.newFixedThreadPool`) runs each `TestStation` on its own thread simultaneously.
- **`LinkedBlockingQueue`** (capacity 512) simulates IPC: producer stations `put()` results and block when the queue is full; the consumer thread `take()`s and blocks when the queue is empty. No explicit `wait()/notify()` boilerplate required.
- **`ReentrantReadWriteLock`** in `AggregationStore` allows multiple concurrent reads while serialising writes — identified as the correct fix during the concurrency bug analysis described below.
- **`synchronized`** blocks guard the observer list in `AggregatorService` (low-frequency operation).

---

### 4. Java RMI Module

`TestResultService` (extends `java.rmi.Remote`) exposes three remote methods:

| Method | Description |
|---|---|
| `getAllResults()` | Full snapshot of all stored results |
| `getResultsForWafer(waferId)` | Per-wafer filtered results |
| `getTotalResultCount()` | Quick count query |

`TestResultServiceImpl` extends `UnicastRemoteObject` for automatic stub generation. `RmiServer.start()` creates a registry on port `1099` and binds the service. `RmiClient.fetchAllResults()` demonstrates the client-side lookup.

---

### 5. gRPC + Protobuf Module

Schema defined in [`src/main/proto/test_result.proto`](src/main/proto/test_result.proto):

```protobuf
service TestResultService {
  rpc GetAllResults   (GetAllResultsRequest)   returns (GetAllResultsResponse);
  rpc GetWaferResults (GetWaferResultsRequest) returns (GetWaferResultsResponse);
  rpc GetSummary      (GetSummaryRequest)      returns (GetSummaryResponse);
  rpc StreamResults   (GetAllResultsRequest)   returns (stream WaferTestResult);
}
```

`GrpcServer.start()` binds `TestResultGrpcService` on port `9090`. `StreamResults` demonstrates server-side streaming — not possible with RMI.

#### RMI vs gRPC Trade-offs

| Aspect | Java RMI | gRPC + Protobuf |
|---|---|---|
| **Language support** | JVM only | Polyglot (Java, Go, Python, C++, …) |
| **Schema / IDL** | Java interfaces + `Serializable` | `.proto` IDL — language-agnostic |
| **Transport** | JRMP over TCP | HTTP/2 (multiplexed, header-compressed) |
| **Versioning** | `serialVersionUID` — fragile; adding a field breaks old clients | Field numbers — backward/forward compatible |
| **Streaming** | Not supported | Server, client, and bidirectional streaming |
| **Performance** | Java serialisation overhead | Binary protobuf encoding — significantly faster |
| **Firewall / proxy** | Uses arbitrary ports, often blocked | HTTP/2 on a single configurable port |
| **Tooling** | Built into JDK, minimal setup | Requires protoc + plugin; more complex build |

**When to choose RMI:** pure-Java ecosystem, quick internal tool, no cross-language requirement, minimal infrastructure.<br>
**When to choose gRPC:** production services, multiple client languages, streaming, high-throughput data, microservices.

---

### 6. Design Patterns

| Pattern | Where | Purpose |
|---|---|---|
| **Factory** | `TestStationFactory` | Creates `TestStation` instances by `StationType` enum without exposing concrete classes |
| **Observer** | `BatchFailureObserver` / `AlertObserver` | `AggregatorService` notifies all registered observers when batch failure rate exceeds the threshold |
| **Singleton** | `SemiTrackLogger`, `AppConfig` | Single shared logger and configuration instance using Bill-Pugh initialisation-on-demand holder |

---

### 7. JUnit Tests

Tests live in `src/test/java/com/semitrack/`:

| Test class | Covers |
|---|---|
| `ValidatorTest` | `CoordinateValidator` and `RangeValidator` — normal paths, boundary values, null inputs, corrupted data (missing keys) |
| `AggregationStoreTest` | CRUD, duplicate die (overwrite semantics), result ordering, concurrent write safety |
| `TestStationFactoryTest` | Correct subtype creation, ID propagation, distinct instances |
| `AggregatorServiceIntegrationTest` | Full submit→validate→store pipeline including consumer thread and `BlockingQueue` |

Run all tests:
```bash
mvn test
```

---

### 8. Spring Boot REST API

Base URL: `http://localhost:8080/api/v1`

| Endpoint | Description |
|---|---|
| `GET /results` | All results; optional `?status=PASS\|FAIL` filter |
| `GET /results/{waferId}` | Results for a specific wafer |
| `GET /summary` | Pass/fail count + yield % per wafer |
| `GET /stats` | System-wide totals, overall yield, average voltage |

Java 8+ streams and lambdas are used throughout `AggregatorController` for filtering, mapping, grouping, and reducing.

---

### 9. Dashboard

Visit `http://localhost:8080/dashboard` after starting the application.

- **KPI cards:** Total dies, passed, failed, yield %, wafer count
- **Bar chart:** Pass/fail breakdown per wafer (Chart.js)
- **Donut chart:** Overall pass/fail distribution
- **Wafer table:** Per-wafer summary with animated yield progress bars
- **Live refresh:** JavaScript `fetch` loop polls the REST API every 10 seconds

Rendered server-side by Spring Boot Thymeleaf on first load, then updated via client-side REST calls.

---

## Concurrency Bug Found and Fixed

### The Bug

During development a subtle live-lock was observed under load. Thread-dump analysis (using `jstack <pid>`) revealed that the `AggregationStore` was originally implemented with a single `synchronized` keyword on every method:

```java
// ORIGINAL – every read blocks all other readers
public synchronized List<WaferTestResult> allResults() { ... }
public synchronized void add(WaferTestResult r) { ... }
```

Under three concurrent test stations each submitting 30 × 30 = 900 results, the REST API's polling calls were being blocked waiting for the write lock even though reads are safe to perform concurrently. The thread dump showed all REST threads stuck at `BLOCKED (on object monitor)` while one write was in progress.

### Thread Dump Evidence

```
"http-nio-8080-exec-3" BLOCKED on com.semitrack.aggregator.AggregationStore@6a3b
"http-nio-8080-exec-4" BLOCKED on com.semitrack.aggregator.AggregationStore@6a3b
"aggregator-consumer"  holds lock on com.semitrack.aggregator.AggregationStore@6a3b
```

### The Fix

Replaced the blanket `synchronized` methods with a `ReentrantReadWriteLock`. Reads acquire the **read lock** (multiple threads can hold it simultaneously); writes acquire the **write lock** (exclusive):

```java
private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

public List<WaferTestResult> allResults() {
    rwLock.readLock().lock();
    try { ... }
    finally { rwLock.readLock().unlock(); }
}

public void add(WaferTestResult result) {
    rwLock.writeLock().lock();
    try { ... }
    finally { rwLock.writeLock().unlock(); }
}
```

After the fix, concurrent read throughput improved proportionally to the number of REST threads, and the blocking behaviour disappeared from thread dumps.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
java -jar target/semitrack-1.0.0.jar
```

The application will:
1. Start the Spring Boot HTTP server on port `8080`
2. Simulate three concurrent test stations (two parametric + one binary)
3. Aggregate results in real time
4. Expose the REST API and dashboard

### Run Tests

```bash
mvn test
```

---

## Project Structure

```
src/
├── main/java/com/semitrack/
│   ├── SemiTrackApplication.java       # Spring Boot entry point + simulation runner
│   ├── config/AppConfig.java           # Singleton configuration
│   ├── logger/SemiTrackLogger.java     # Singleton logger
│   ├── model/                          # Domain objects (WaferDie, WaferTestResult, enums)
│   ├── station/                        # Abstract TestStation + concrete implementations
│   ├── validator/                      # Validator interface + CoordinateValidator + RangeValidator
│   ├── report/                         # ReportGenerator interface + Console + JSON impls
│   ├── aggregator/                     # AggregationStore (ReadWriteLock) + AggregatorService
│   ├── factory/                        # TestStationFactory (Factory pattern)
│   ├── observer/                       # BatchFailureObserver interface + AlertObserver
│   ├── rmi/                            # Java RMI server, impl, and client
│   ├── grpc/                           # gRPC service, server, and client
│   └── rest/                           # Spring MVC REST + Thymeleaf dashboard controllers
├── main/proto/test_result.proto        # Protobuf schema for gRPC
├── main/resources/
│   ├── application.properties
│   └── templates/dashboard.html       # Thymeleaf + Chart.js dashboard
└── test/java/com/semitrack/
    ├── validator/ValidatorTest.java
    ├── aggregator/AggregationStoreTest.java
    ├── aggregator/AggregatorServiceIntegrationTest.java
    └── station/TestStationFactoryTest.java
```

---

## Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 17 | Language runtime |
| Spring Boot | 3.2.3 | REST API, Thymeleaf, dependency injection |
| Maven | 3.9+ | Build tool, dependency management |
| JUnit Jupiter | 5.x | Unit and integration testing |
| gRPC | 1.62.2 | High-performance RPC framework |
| Protobuf | 3.25.3 | Binary serialisation / IDL |
| Java RMI | JDK built-in | Distributed Java object invocation |
| Jackson | 2.x | JSON serialisation |
| Chart.js | 4.4.2 | Dashboard visualisation |
