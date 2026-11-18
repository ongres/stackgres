---
title: Babelfish for PostgreSQL
weight: 18
url: /features/babelfish
description: T-SQL compatibility layer for PostgreSQL using Babelfish.
---

StackGres supports [Babelfish for PostgreSQL](https://babelfishpg.org/), an open-source project that adds T-SQL compatibility to PostgreSQL. This allows applications written for Microsoft SQL Server to run on PostgreSQL with minimal code changes.

## What is Babelfish?

Babelfish provides:

- **T-SQL Support**: Execute T-SQL queries, stored procedures, and functions
- **TDS Protocol**: Native SQL Server wire protocol support (port 1433)
- **SQL Server Semantics**: Compatible behavior for common SQL Server patterns
- **Dual Access**: Connect via TDS (SQL Server) or PostgreSQL protocol simultaneously

## Feature Gate

Babelfish is available as a non-production feature gate. To enable it, use the `enabledFeatureGates` configuration:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: babelfish-cluster
spec:
  postgres:
    flavor: babelfish
  nonProductionOptions:
    enabledFeatureGates:
      - babelfish-flavor
```

> **Warning**: Babelfish is currently a non-production feature. It is recommended for testing and development environments only.

## Use Cases

Babelfish is ideal for:

- **Migration Projects**: Test SQL Server applications against PostgreSQL
- **Hybrid Environments**: Run both SQL Server and PostgreSQL workloads
- **Development**: Develop with PostgreSQL while targeting SQL Server compatibility
- **Cost Reduction**: Evaluate moving from SQL Server to PostgreSQL

## Limitations

Current limitations include:

- Non-production feature status
- Not all T-SQL features are supported
- Some SQL Server system procedures may not be available
- Performance characteristics may differ from native SQL Server

## Getting Started

For detailed setup instructions, see the [Babelfish Configuration Guide]({{% relref "04-administration-guide/19-babelfish" %}}).

## Related Resources

- [Babelfish Project](https://babelfishpg.org/)
- [Babelfish Documentation](https://babelfishpg.org/docs/)
- [SQL Server Compatibility](https://babelfishpg.org/docs/usage/compatibility/)
