# Architecture Diagram

This document summarizes the current ZavaLedger architecture at the application and component levels.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        Browser["Banking API Client"]
    end
    subgraph App["Application Layer - Java Servlet 4.0"]
        AccountApi["AccountServlet"]
        TxApi["TransactionServlet"]
        HealthApi["HealthServlet"]
        Config["LedgerConfig"]
        ConnFactory["LedgerConnectionFactory"]
    end
    subgraph Data["Data Layer"]
        SqlJdbc["Microsoft SQLServer JDBC Driver"]
        SqlServer[("SQL Server")]
    end
    subgraph External["External Systems"]
        DockerRuntime["Tomcat 9 Container"]
    end

    Browser -->|"HTTP/XML requests"| AccountApi
    Browser -->|"HTTP/XML requests"| TxApi
    Browser -->|"HTTP health check"| HealthApi
    AccountApi -->|"DB reads"| ConnFactory
    TxApi -->|"DB reads/writes"| ConnFactory
    ConnFactory -->|"JDBC connection"| SqlJdbc
    SqlJdbc -->|"TDS queries"| SqlServer
    Config -->|"DB settings"| ConnFactory
    DockerRuntime -->|"hosts servlet app"| AccountApi
    DockerRuntime -->|"hosts servlet app"| TxApi
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Presentation | Java Servlets + JSP | Servlet 4.0 | Exposes HTTP endpoints and simple UI |
| Business Logic | Java classes in `com.zavabank.ledger` | Java 17 | Request parsing and ledger transaction logic |
| Data Access | JDBC via SQL Server driver | mssql-jdbc 12.8.1.jre11 | Executes account and transaction SQL operations |
| Runtime | Apache Tomcat container | 9.0-jdk17 | Hosts WAR deployment |
| Build | Gradle | 7.x/9.x compatible build scripts | Builds WAR artifact |

### Data Storage & External Services

The application stores account and transaction data in a SQL Server database (`Accounts`, `Transactions`, and `TransactionTypes` tables). It has no cache or message broker dependencies and relies on JDBC over the SQL Server driver for all data access.

### Key Architectural Decisions

- Uses a straightforward servlet-based monolithic architecture with XML request/response payloads.
- Centralizes DB connection construction in `LedgerConnectionFactory` and environment/property resolution in `LedgerConfig`.
- Uses prepared SQL statements for database interactions in account and transaction endpoints.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation
        HealthServlet["HealthServlet"]
        AccountServlet["AccountServlet"]
        TransactionServlet["TransactionServlet"]
    end
    subgraph Business["Business Logic"]
        RequestParsing["XML Request Parsing"]
        LedgerXml["LedgerXml"]
        LedgerConfig["LedgerConfig"]
    end
    subgraph DataAccess["Data Access"]
        ConnectionFactory["LedgerConnectionFactory"]
        AccountQueries["Account SQL Queries"]
        TransactionQueries["Transaction SQL Queries"]
    end
    subgraph Infrastructure
        WebXml["web.xml Routing"]
        JdbcDriver["SQLServer JDBC Driver"]
    end

    WebXml -->|"maps routes"| HealthServlet
    WebXml -->|"maps routes"| AccountServlet
    WebXml -->|"maps routes"| TransactionServlet
    TransactionServlet -->|"parses XML"| RequestParsing
    AccountServlet -->|"escapes output"| LedgerXml
    TransactionServlet -->|"escapes output"| LedgerXml
    AccountServlet -->|"gets DB connection"| ConnectionFactory
    TransactionServlet -->|"gets DB connection"| ConnectionFactory
    LedgerConfig -->|"provides DB config"| ConnectionFactory
    ConnectionFactory -->|"executes"| AccountQueries
    ConnectionFactory -->|"executes"| TransactionQueries
    ConnectionFactory -->|"uses"| JdbcDriver
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| `HealthServlet` | Presentation | Servlet | Returns service health response |
| `AccountServlet` | Presentation | Servlet | Serves account balance and transaction history endpoints |
| `TransactionServlet` | Presentation | Servlet | Processes posted transactions and writes ledger entries |
| `LedgerXml` | Business Logic | Utility | Escapes XML output values |
| `LedgerConfig` | Business Logic | Configuration utility | Resolves DB settings from env vars/properties |
| `LedgerConnectionFactory` | Data Access | Factory | Creates JDBC connections |
| SQL query blocks in servlets | Data Access | JDBC statements | Reads/updates account and transaction tables |
| `web.xml` | Infrastructure | Servlet descriptor | Maps endpoint paths to servlet classes |
