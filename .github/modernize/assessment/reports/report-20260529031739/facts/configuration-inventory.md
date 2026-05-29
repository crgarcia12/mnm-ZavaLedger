# Configuration Inventory

This inventory summarizes externalized and embedded runtime configuration for ZavaLedger, including configuration sources, runtime parameters, and sensitive settings.

## Configuration Sources

| Source | Location | Scope | Notes |
|---|---|---|---|
| Java properties file | `src/main/resources/ledger.properties` | Runtime defaults | Provides DB host, port, name, user, and password defaults |
| Environment variables | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Runtime override | Preferred source when present |
| Build configuration | `build.gradle` | Build-time | Declares Java level and dependencies |
| Container configuration | `Dockerfile` | Deployment | Builds WAR and deploys to Tomcat on port 8080 |

## Build Profiles

| Profile | Activation | Purpose |
|---|---|---|
| default | Always | Standard Gradle Java/WAR build |

## Runtime Profiles

| Profile | Data Source Behavior | Notes |
|---|---|---|
| default | SQL Server settings from env vars, fallback to properties file | No explicit multi-profile runtime config detected |

## Properties Inventory

| Property/Setting | Source | Category | Effective Behavior |
|---|---|---|---|
| DB_HOST / db.host | env + properties | Database | Resolves SQL Server host |
| DB_PORT / db.port | env + properties | Database | Resolves SQL Server port |
| DB_NAME / db.name | env + properties | Database | Resolves database name |
| DB_USER / db.user | env + properties | Credentials | Resolves DB user |
| DB_PASSWORD / db.password | env + properties | Credentials | Resolves DB password |

## Startup Parameters & Resource Requirements

| Parameter | Value | Notes |
|---|---|---|
| Java target level | 17 | Declared in Gradle build |
| Exposed HTTP port | 8080 | Tomcat container port |
| Artifact packaging | ROOT.war | Deployed as default webapp |

## Startup Dependency Chain

Application startup depends on Tomcat loading `web.xml`, servlet initialization, SQL Server JDBC driver availability, and network reachability to the configured SQL Server endpoint.

## Secrets & Sensitive Configuration

| Item | Location | Handling | Risk |
|---|---|---|---|
| Database password default | `src/main/resources/ledger.properties` | Stored in plaintext | High |
| Database user default | `src/main/resources/ledger.properties` | Stored in plaintext | Medium |

## Feature Flags

No feature flag system or toggle configuration was detected.

## Framework & Runtime Versions

| Component | Version | Source |
|---|---|---|
| Java | 17 | `build.gradle` |
| Servlet API | 4.0.1 | `build.gradle` |
| SQL Server JDBC Driver | 12.8.1.jre11 | `build.gradle` |
| Tomcat | 9.0 (base image tag) | `Dockerfile` |
