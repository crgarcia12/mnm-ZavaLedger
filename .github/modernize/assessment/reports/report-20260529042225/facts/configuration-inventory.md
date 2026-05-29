# Configuration Inventory

| Key | Source | Default/Observed Value | Purpose |
|---|---|---|---|
| `DB_HOST` / `db.host` | Env var or `ledger.properties` | `sqlserver` | SQL Server host |
| `DB_PORT` / `db.port` | Env var or `ledger.properties` | `1433` | SQL Server port |
| `DB_NAME` / `db.name` | Env var or `ledger.properties` | `ZavaBankDB` | Database name |
| `DB_USER` / `db.user` | Env var or `ledger.properties` | `sa` | Database username |
| `DB_PASSWORD` / `db.password` | Env var or `ledger.properties` | set in properties | Database password |

Other runtime config:
- `web.xml` defines servlet routing for `/health`, `/api/accounts/*`, and `/api/transactions`.
- Dockerfile builds WAR with Gradle and deploys to Tomcat 9 (`EXPOSE 8080`).
