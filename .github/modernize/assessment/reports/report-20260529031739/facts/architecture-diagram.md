# Architecture Diagram

This document summarizes the current ZavaLedger runtime architecture and its core component relationships.

## Application Architecture

```mermaid
flowchart TD
    subgraph Client["Client Layer"]
        ClientApp["Bank Channel Client"]
    end
    subgraph App["Application Layer - Java Servlet"]
        WebXml["web.xml Routing"]
        TxServlet["TransactionServlet"]
        AcctServlet["AccountServlet"]
        HealthServlet["HealthServlet"]
    end
    subgraph DataLayer["Data Layer"]
        ConnFactory["LedgerConnectionFactory"]
        Config["LedgerConfig"]
        SQLServer[("Microsoft SQL Server")]
    end
    subgraph External["External Services"]
        EnvVars["Environment Variables"]
    end

    ClientApp -->|"HTTP XML requests"| WebXml
    WebXml -->|"POST /api/transactions"| TxServlet
    WebXml -->|"GET /api/accounts/*"| AcctServlet
    WebXml -->|"GET /health"| HealthServlet
    TxServlet -->|"JDBC operations"| ConnFactory
    AcctServlet -->|"JDBC queries"| ConnFactory
    ConnFactory -->|"DB credentials"| Config
    Config -->|"fallback values"| EnvVars
    ConnFactory -->|"SQL queries"| SQLServer
```

### Technology Stack Summary

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Presentation | Java Servlet API | 4.0.1 | Exposes HTTP endpoints via servlets |
| Build | Gradle | N/A (wrapper not committed) | Builds WAR artifact |
| Data Access | Microsoft JDBC Driver | 12.8.1.jre11 | SQL Server connectivity |
| Runtime | Apache Tomcat | 9.0 (Docker image) | Hosts ROOT.war |
| Database | Microsoft SQL Server | Not pinned in repo | Stores accounts and transactions |

### Data Storage & External Services

The application persists ledger data in Microsoft SQL Server and accesses it through plain JDBC statements. It also depends on environment-provided connection settings, with local defaults loaded from `ledger.properties` when environment variables are absent.

### Key Architectural Decisions

- Uses a servlet-based monolith packaged as a single WAR and deployed to Tomcat.
- Uses direct JDBC data access without a repository/ORM abstraction.
- Uses XML request/response payloads for transaction and account APIs.

## Component Relationships

```mermaid
flowchart LR
    subgraph Presentation["Presentation"]
        Health["HealthServlet"]
        Tx["TransactionServlet"]
        Acct["AccountServlet"]
    end
    subgraph Business["Business Logic"]
        TxLogic["Transaction posting logic"]
        AcctLogic["Balance and transaction lookup logic"]
        XmlUtil["LedgerXml"]
    end
    subgraph DataAccess["Data Access"]
        Conn["LedgerConnectionFactory"]
        Cfg["LedgerConfig"]
    end
    subgraph Infra["Infrastructure"]
        Sql[("SQL Server")]
    end

    Tx -->|"delegates"| TxLogic
    Acct -->|"delegates"| AcctLogic
    TxLogic -->|"escapes XML"| XmlUtil
    AcctLogic -->|"escapes XML"| XmlUtil
    TxLogic -->|"opens connection"| Conn
    AcctLogic -->|"opens connection"| Conn
    Conn -->|"reads config"| Cfg
    Conn -->|"executes SQL"| Sql
```

### Component Inventory

| Component | Layer | Type | Responsibility |
|---|---|---|---|
| HealthServlet | Presentation | Servlet | Returns application health response |
| TransactionServlet | Presentation | Servlet | Parses transaction XML and posts ledger entries |
| AccountServlet | Presentation | Servlet | Serves account balance and transaction history APIs |
| LedgerXml | Business Logic | Utility | Escapes XML output content |
| LedgerConnectionFactory | Data Access | Factory | Creates JDBC connections to SQL Server |
| LedgerConfig | Data Access | Configuration | Resolves DB settings from environment or properties |
