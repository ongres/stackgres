---
title: Extension Versions
weight: 2
url: /administration/extensions/versions
description: How extension versioning and channels work in StackGres.
showToc: true
---

StackGres manages PostgreSQL extension versions through a channel system that provides flexibility while ensuring compatibility.

## Version Channel System

When specifying extensions, you can use different version formats:

| Format | Example | Behavior |
|--------|---------|----------|
| Omitted | - | Uses `stable` channel (latest stable version) |
| Channel | `stable` | Latest version from the specified channel |
| Specific | `1.5.0` | Exact version |

### Default Behavior

When you don't specify a version, StackGres uses the `stable` channel:

```yaml
spec:
  postgres:
    extensions:
      - name: postgis  # Uses stable channel
```

This is equivalent to:

```yaml
spec:
  postgres:
    extensions:
      - name: postgis
        version: stable
```

### Using Specific Versions

Pin to a specific version for reproducible deployments:

```yaml
spec:
  postgres:
    extensions:
      - name: postgis
        version: '3.4.0'
```

## Extension Metadata

Each extension in the repository has metadata including:

- **name**: Extension name
- **publisher**: Publisher ID (default: `com.ongres`)
- **version**: Version string
- **postgresVersion**: Compatible PostgreSQL major version
- **build**: Build version (e.g., `15.1`, `16.2`)

### Viewing Available Versions

Check available versions in the [Extensions Catalog]({{% relref "01-introduction/08-extensions" %}}) or query the repository directly.

### Build Versions

Extensions are built for specific PostgreSQL versions. The build version follows the pattern:

```
<postgres_major>.<build_iteration>
```

For example, `16.2` means:
- Built for PostgreSQL 16
- Second build iteration for that major version

## Publisher System

Extensions can have different publishers:

```yaml
spec:
  postgres:
    extensions:
      - name: my-extension
        publisher: com.mycompany
        version: '1.0.0'
```

The default publisher is `com.ongres`, which hosts the standard StackGres extensions.

## Version Resolution

When StackGres resolves an extension version:

1. **Check specified version**: If a specific version is given, use it
2. **Check channel**: If a channel name is given, resolve to latest in that channel
3. **Default to stable**: If nothing specified, use the stable channel
4. **Match PostgreSQL version**: Filter for compatible PostgreSQL major version
5. **Select latest build**: Choose the most recent build for the PostgreSQL version

### Resolution Example

For this configuration:

```yaml
spec:
  postgres:
    version: '16'
    extensions:
      - name: postgis
```

StackGres will:
1. Look for `postgis` with `stable` channel
2. Filter for PostgreSQL 16 compatible versions
3. Select the latest stable version for PG 16

## Pinning Versions

### Why Pin Versions?

- **Reproducibility**: Same extension version across environments
- **Stability**: Avoid unexpected changes from updates
- **Testing**: Verify compatibility before upgrading

### Best Practices for Version Pinning

```yaml
# Production: Pin specific versions
spec:
  postgres:
    extensions:
      - name: postgis
        version: '3.4.0'
      - name: pgvector
        version: '0.5.1'

# Development: Use stable channel for latest
spec:
  postgres:
    extensions:
      - name: postgis
        # version omitted = stable channel
```

## Checking Installed Versions

### Via Status

Check the cluster status for installed extensions:

```bash
kubectl get sgcluster my-cluster -o jsonpath='{.status.extensions}' | jq
```

### Via PostgreSQL

Query installed extensions:

```bash
kubectl exec my-cluster-0 -c postgres-util -- psql -c "SELECT * FROM pg_available_extensions WHERE installed_version IS NOT NULL"
```

## Version Upgrade Process

When you change an extension version:

1. StackGres detects the version change
2. New extension version is downloaded
3. Extension is upgraded using `ALTER EXTENSION ... UPDATE`
4. Some extensions may require a cluster restart

### Upgrade Example

```yaml
# Before
spec:
  postgres:
    extensions:
      - name: postgis
        version: '3.3.0'

# After
spec:
  postgres:
    extensions:
      - name: postgis
        version: '3.4.0'
```

Apply the change:

```bash
kubectl apply -f sgcluster.yaml
```

### Checking if Restart Required

Some extension upgrades require a restart:

```bash
kubectl get sgcluster my-cluster -o jsonpath='{.status.conditions}'
```

Look for the `PendingRestart` condition.

## Downgrading Extensions

Extension downgrades are generally not supported by PostgreSQL. To "downgrade":

1. Remove the extension from the cluster spec
2. Manually run `DROP EXTENSION` in PostgreSQL
3. Add the extension back with the desired version

## Custom Repositories

### Configuring Custom Repository

Add custom extension repositories in SGConfig:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
spec:
  extensions:
    repositoryUrls:
      - https://extensions.stackgres.io/postgres/repository
      - https://my-company.example.com/extensions/repository
```

### Repository URL Parameters

Customize repository access:

```yaml
spec:
  extensions:
    repositoryUrls:
      - https://extensions.example.com/repo?proxyUrl=http%3A%2F%2Fproxy%3A8080&retry=3:5000
```

Parameters:
- `proxyUrl`: HTTP proxy (URL-encoded)
- `skipHostnameVerification`: Skip TLS verification (`true`/`false`)
- `retry`: Retry config (`<max_retries>:<sleep_ms>`)

