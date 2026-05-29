# Modernization Plan: modernization-plan

**Project**: ZavaLedger

---

## Technical Framework

- **Language**: Java 17
- **Framework**: Java Servlet 4.0 WAR application on Tomcat 9
- **Build Tool**: Gradle
- **Database**: Microsoft SQL Server via JDBC
- **Key Dependencies**: javax.servlet-api 4.0.1, mssql-jdbc 12.8.1.jre11

---

## Overview

> This migration modernizes the ZavaLedger application for Azure. The
> application currently runs as a traditional Java WAR on Tomcat with SQL
> Server connectivity configured through local properties and environment
> variables. The new architecture will:
>
> - move application hosting to Azure for simpler operations and scaling
> - align the relational data tier with Azure-managed services
> - centralize secret handling and dependency remediation for safer releases
>
> The migration follows a phased approach that first aligns data access and
> configuration with Azure services, then validates security posture, and
> finally deploys the application to Azure.

---

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| ZavaLedger | Tomcat-hosted WAR app | Azure Container Apps | Managed Identity | Host the existing containerized web app on Azure |
| ZavaLedger | SQL Server + local secrets | Azure SQL Database + Key Vault | Managed Identity | Move database access and secret storage to Azure-managed services |

---

## Open Questions & Questionnaire

- [ ] Which Azure subscription, resource group, and region should host the workload?
- [ ] Should deployment target new Azure resources or reuse existing ones?
- [ ] Should database access use managed identity end-to-end or retain SQL authentication temporarily?

---

## Security Compliance

**Description**: Scan all project dependencies for known CVEs and remediate any
identified vulnerabilities to ensure the application is secure before
deployment.

**Requirements**:
  Upgrade vulnerable dependencies to the minimum patched version. If a CVE fix
  requires a major version upgrade, document the affected dependency, the
  current version, the upgraded major version, and the breaking change risk.
  Verify that the project builds and all tests pass after remediation.

**Environment Configuration**:
  Runtime environment established by previous tasks.
  Build tool established by previous tasks.

**App Scope**:
  /tmp/workspace/crgarcia12/mnm-ZavaLedger

**Skills**:
  - Skill Name: validate-cves-and-fix
    - Skill Location: builtin
