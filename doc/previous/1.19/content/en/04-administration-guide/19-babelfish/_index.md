---
title: Babelfish Configuration
weight: 19
url: /administration/babelfish
description: How to configure and use Babelfish for PostgreSQL in StackGres.
showToc: true
---

This guide covers how to enable and configure Babelfish for PostgreSQL in StackGres clusters, providing T-SQL and TDS protocol compatibility.

> **Warning**: Babelfish is a non-production feature. Use it for testing and development only.

## Prerequisites

- StackGres operator installed
- Understanding of SQL Server T-SQL syntax
- Familiarity with PostgreSQL

## Enabling Babelfish

### Step 1: Create the Cluster

Create an SGCluster with the Babelfish flavor enabled:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: babelfish-cluster
spec:
  instances: 2
  postgres:
    version: '15'
    flavor: babelfish
  pods:
    persistentVolume:
      size: '10Gi'
  nonProductionOptions:
    enabledFeatureGates:
      - babelfish-flavor
```

Apply the configuration:

```bash
kubectl apply -f babelfish-cluster.yaml
```

### Step 2: Verify Installation

Check that the cluster is running with Babelfish:

```bash
kubectl get sgcluster babelfish-cluster
```

Verify Babelfish extensions are installed:

```bash
kubectl exec babelfish-cluster-0 -c postgres-util -- psql -c \
  "SELECT * FROM pg_extension WHERE extname LIKE 'babelfishpg%'"
```

## Connecting to Babelfish

### TDS Protocol (SQL Server Compatible)

Connect using SQL Server tools on port 1433:

```bash
# Using sqlcmd
sqlcmd -S babelfish-cluster,1433 -U postgres -P <password>

# Using Azure Data Studio or SSMS
# Server: babelfish-cluster
# Port: 1433
# Authentication: SQL Server Authentication
```

### PostgreSQL Protocol

Connect using standard PostgreSQL tools:

```bash
kubectl exec babelfish-cluster-0 -c postgres-util -- psql
```

## Configuration Options

### Babelfish-Specific Settings

Configure Babelfish behavior via SGPostgresConfig:

```yaml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: babelfish-config
spec:
  postgresVersion: "15"
  postgresql.conf:
    # Babelfish settings
    babelfishpg_tsql.database_name: 'master'
    babelfishpg_tsql.migration_mode: 'single-db'
    babelfishpg_tsql.default_locale: 'en_US.UTF-8'
```

### Migration Modes

Babelfish supports different migration modes:

| Mode | Description |
|------|-------------|
| `single-db` | All SQL Server databases map to one PostgreSQL database |
| `multi-db` | Each SQL Server database maps to a PostgreSQL schema |

```yaml
babelfishpg_tsql.migration_mode: 'multi-db'
```

## Creating SQL Server Databases

After connecting via TDS:

```sql
-- Create a database (maps to PostgreSQL schema)
CREATE DATABASE myapp;
GO

-- Use the database
USE myapp;
GO

-- Create a table
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT GETDATE()
);
GO
```

## T-SQL Features

### Supported Features

- **DDL**: CREATE/ALTER/DROP TABLE, VIEW, PROCEDURE, FUNCTION
- **DML**: SELECT, INSERT, UPDATE, DELETE with T-SQL syntax
- **Transactions**: BEGIN TRAN, COMMIT, ROLLBACK
- **Control Flow**: IF/ELSE, WHILE, TRY/CATCH
- **Built-in Functions**: Many SQL Server functions supported
- **Data Types**: Common SQL Server types (NVARCHAR, DATETIME, etc.)

### Example T-SQL Procedure

```sql
CREATE PROCEDURE GetUserById
    @UserId INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT id, username, created_at
    FROM users
    WHERE id = @UserId;
END;
GO
```

### Example T-SQL Query

```sql
-- SQL Server style query
SELECT TOP 10
    u.username,
    CONVERT(VARCHAR(10), u.created_at, 120) AS created_date
FROM users u
WHERE u.created_at > DATEADD(day, -30, GETDATE())
ORDER BY u.created_at DESC;
```

## Exposing TDS Port

To access Babelfish externally, expose the TDS port:

### Via LoadBalancer

```yaml
apiVersion: v1
kind: Service
metadata:
  name: babelfish-tds
spec:
  type: LoadBalancer
  selector:
    app: StackGresCluster
    stackgres.io/cluster-name: babelfish-cluster
    role: master
  ports:
    - port: 1433
      targetPort: 1433
      name: tds
```

### Via Port Forward

```bash
kubectl port-forward svc/babelfish-cluster 1433:1433
```

## Migration from SQL Server

### Step 1: Export Schema

Use SQL Server tools to export schema:

```bash
# Using mssql-scripter
mssql-scripter -S sqlserver.example.com -d mydb -U sa -P password \
  --schema-only > schema.sql
```

### Step 2: Review Compatibility

Check for unsupported features:
- Review stored procedures for unsupported syntax
- Check for SQL Server-specific features
- Test queries in Babelfish

### Step 3: Import Schema

Connect via TDS and run the schema script:

```bash
sqlcmd -S babelfish-cluster,1433 -U postgres -P <password> -i schema.sql
```

### Step 4: Migrate Data

Use standard tools to migrate data:

```bash
# Export from SQL Server
bcp mydb.dbo.users out users.dat -S sqlserver -U sa -P password -n

# Import to Babelfish
bcp mydb.dbo.users in users.dat -S babelfish-cluster,1433 -U postgres -P password -n
```

## Compatibility Checking

### Check Supported Features

Query the Babelfish compatibility views:

```sql
-- Via PostgreSQL
SELECT * FROM babelfish_sysdatabases;
SELECT * FROM babelfish_authid_login_ext;
```

### Test Queries

Before full migration, test critical queries:

```sql
-- Test stored procedures
EXEC GetUserById @UserId = 1;

-- Test complex queries
SELECT * FROM information_schema.tables;
```

## Monitoring

### Connection Metrics

Monitor TDS connections:

```sql
-- Active connections
SELECT * FROM pg_stat_activity
WHERE application_name LIKE '%tds%';
```

### Performance

Use standard PostgreSQL monitoring plus Babelfish-specific views:

```sql
-- Query statistics
SELECT * FROM pg_stat_statements
WHERE query LIKE '%SELECT%';
```

## Limitations and Workarounds

### Unsupported Features

Some SQL Server features are not supported:

| Feature | Status | Workaround |
|---------|--------|------------|
| SQLCLR | Not supported | Rewrite in PL/pgSQL |
| Linked Servers | Not supported | Use foreign data wrappers |
| Full-text Search | Limited | Use PostgreSQL FTS |
| Service Broker | Not supported | Use alternative messaging |

### Data Type Mappings

Some types map differently:

| SQL Server | PostgreSQL |
|------------|------------|
| NVARCHAR | VARCHAR (UTF-8) |
| DATETIME | TIMESTAMP |
| MONEY | NUMERIC(19,4) |
| BIT | BOOLEAN |

## Best Practices

1. **Test thoroughly**: Run comprehensive tests before migration
2. **Start with single-db mode**: Simpler setup for initial testing
3. **Use PostgreSQL protocol for admin**: Better tooling and compatibility
4. **Monitor both protocols**: Track TDS and PostgreSQL connections
5. **Plan for differences**: Some behavior may differ from SQL Server

## Related Documentation

- [Babelfish Feature]({{% relref "02-features/18-babelfish" %}})
- [Babelfish Project](https://babelfishpg.org/)
- [PostgreSQL Configuration]({{% relref "04-administration-guide/04-configuration/02-postgres-configuration" %}})
