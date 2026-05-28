# Security Assessment Report

**Generated:** 2026-05-28T23:21:12.0000000Z

## Summary

| Metric | Count |
|--------|-------|
| Total Findings | 6 |
| CVE Vulnerabilities | 0 |
| CWE Vulnerabilities | 6 |
| Total Rules Assessed | 59 |
| Rules Passed | 53 |

### By Severity

| Severity | Count |
|----------|-------|
| mandatory | 0 |
| optional | 3 |
| potential | 3 |

## CVE Findings (Dependency Vulnerabilities)

No CVE vulnerabilities found meeting the minimum severity threshold (high).

## CWE Findings (Code-Level Vulnerabilities)

### CWE-477: Use of Obsolete Function
- **Category:** Code Quality
- **Severity:** optional
- **Story Points:** 1
- **Files:** src/main/java/com/zavabank/ledger/LedgerConnectionFactory.java

In LedgerConnectionFactory (line 10), Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver") is used to manually register the JDBC driver. This practice has been obsolete since JDBC 4.0 (Java SE 6), which introduced automatic driver discovery via the ServiceLoader mechanism. Modern JDBC drivers, including mssql-jdbc, register themselves automatically through META-INF/services without requiring an explicit Class.forName() call.

### CWE-772: Missing Release of Resource after Effective Lifetime
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/ledger/TransactionServlet.java

In TransactionServlet, several helper methods (readBalance at line 144, findTransactionType at line 157, updateAccountBalance at line 174, insertTransaction at line 186) create PreparedStatement and ResultSet objects but close them inline without wrapping in try-finally blocks. If a SQLException is thrown before the close() calls are reached, these JDBC resources may not be released immediately. For example, in readBalance(), if resultSet.next() or getBigDecimal() throws, both resultSet.close() (line 152) and statement.close() (line 153) are skipped. Although the outer connection.close() in postTransaction()'s finally block (line 134) may cascade-close associated statements, relying on this behavior is implementation-dependent.

### CWE-1057: Data Access Operations Outside of Expected Data Manager Component
- **Category:** Code Quality
- **Severity:** potential
- **Story Points:** 5
- **Files:** src/main/java/com/zavabank/ledger/AccountServlet.java, src/main/java/com/zavabank/ledger/TransactionServlet.java

The application has LedgerConnectionFactory as a connection-management component, but all SQL data access operations (SELECT, UPDATE, INSERT) are performed directly inside servlet classes. AccountServlet (lines 62, 98) and TransactionServlet (lines 145, 158, 175, 195) construct and execute SQL queries inline without delegating to a dedicated data access layer (e.g., DAO or Repository). This bypasses any centralized data management and mixes business logic with data access within the presentation layer (servlets).

### CWE-259: Use of Hard-coded Password
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/ledger.properties

The file src/main/resources/ledger.properties (line 5) contains a hard-coded database password: db.****** This password is stored as a plain-text default in the bundled properties file. While LedgerConfig.read() does allow the password to be overridden via the DB_PASSWORD environment variable, shipping a hard-coded password as a fallback in the packaged artifact poses a security risk if the environment variable is not set in production.

### CWE-778: Insufficient Logging
- **Category:** Credentials & Secrets
- **Severity:** potential
- **Story Points:** 3
- **Files:** src/main/java/com/zavabank/ledger/AccountServlet.java, src/main/java/com/zavabank/ledger/TransactionServlet.java, src/main/java/com/zavabank/ledger/LedgerConfig.java

The application contains no logging framework (no SLF4J, Log4j, or java.util.logging). Security-critical events are silently discarded: (1) AccountServlet catches SQLException at line 82 and 121 without any logging, returning only a generic error response; (2) TransactionServlet catches all Exception at line 31 and only returns a generic error XML with no record of the failure; (3) LedgerConfig silently ignores an IOException when loading the properties file (line 17), making configuration failures undetectable; (4) Financial transaction failures (SQLException/IllegalArgumentException in TransactionServlet.postTransaction(), lines 126-130) are rethrown but never logged before or after the rollback. Failed authentication attempts, database errors, and financial transaction anomalies are not recorded anywhere.

### CWE-798: Use of Hard-coded Credentials
- **Category:** Credentials & Secrets
- **Severity:** optional
- **Story Points:** 5
- **Files:** src/main/resources/ledger.properties

The bundled configuration file src/main/resources/ledger.properties contains hard-coded database credentials: db.user=sa (line 4) and db.****** (line 5). The 'sa' (system administrator) account has full DBA privileges on SQL Server, making the exposure of these credentials especially critical. Although LedgerConfig.read() checks for DB_USER and DB_PASSWORD environment variables first, these hard-coded values are packaged into the WAR artifact and serve as insecure fallback defaults.
