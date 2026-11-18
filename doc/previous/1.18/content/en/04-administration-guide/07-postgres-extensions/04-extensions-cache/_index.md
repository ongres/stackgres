---
title: Extensions Cache
weight: 4
url: /administration/extensions/cache
description: How to configure the extensions cache for faster deployments.
showToc: true
---

StackGres can cache PostgreSQL extensions locally to speed up cluster deployments and reduce external network dependencies.

> **Note**: The extensions cache is an experimental feature.

## Overview

Without caching, each cluster pod downloads extensions from the repository when starting. The extensions cache stores downloaded extensions locally, providing:

- **Faster deployments**: No need to download from external repository
- **Reduced bandwidth**: Download once, use many times
- **Offline capability**: Deploy clusters without internet access (with pre-loaded cache)
- **Consistency**: All clusters use the same cached extension binaries

## Enabling the Cache

### Via SGConfig

Configure the cache in the SGConfig resource:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  extensions:
    cache:
      enabled: true
      persistentVolume:
        size: 10Gi
        storageClass: fast-storage
```

### Via Helm Values

Enable during operator installation:

```yaml
# values.yaml
extensions:
  cache:
    enabled: true
    persistentVolume:
      size: 10Gi
```

```bash
helm install stackgres-operator stackgres-charts/stackgres-operator \
  -f values.yaml
```

## Cache Configuration Options

### Persistent Volume

Use a PersistentVolumeClaim for cache storage:

```yaml
spec:
  extensions:
    cache:
      enabled: true
      persistentVolume:
        size: 20Gi
        storageClass: standard
```

### Host Path (Not Recommended)

For testing only, use a host path:

```yaml
spec:
  extensions:
    cache:
      enabled: true
      hostPath: /var/cache/stackgres/extensions
```

> **Warning**: Host path is not suitable for production as it doesn't survive node failures.

## Pre-Loading Extensions

Pre-load commonly used extensions into the cache:

```yaml
spec:
  extensions:
    cache:
      enabled: true
      preLoadedExtensions:
        - postgis
        - pgvector
        - timescaledb
      persistentVolume:
        size: 20Gi
```

### Pre-Load Patterns

Use patterns to pre-load multiple extensions:

```yaml
preLoadedExtensions:
  - postgis           # Specific extension
  - pg*               # All extensions starting with 'pg'
  - "*vector*"        # All extensions containing 'vector'
```

## How It Works

1. **First Request**: When a cluster needs an extension:
   - Cache checks if extension is available locally
   - If not, downloads from repository and stores in cache
   - Extension is provided to the cluster

2. **Subsequent Requests**: For the same extension:
   - Cache serves extension directly from local storage
   - No external network request needed

3. **Cache Invalidation**: Extensions are cached by version
   - Different versions are cached separately
   - Updating extension version downloads new version

## Monitoring the Cache

### Check Cache Status

```bash
# View cache pod
kubectl get pods -n stackgres -l app=stackgres-extensions-cache

# Check cache PVC
kubectl get pvc -n stackgres | grep extensions-cache

# View cache logs
kubectl logs -n stackgres -l app=stackgres-extensions-cache
```

### Cache Size

Monitor cache disk usage:

```bash
kubectl exec -n stackgres -l app=stackgres-extensions-cache -- \
  du -sh /var/cache/extensions
```

## Offline Deployments

For air-gapped environments:

### Step 1: Pre-Load Cache Online

On a connected environment:

```yaml
spec:
  extensions:
    cache:
      enabled: true
      preLoadedExtensions:
        - postgis
        - pgvector
        - timescaledb
        - pg_stat_statements
      persistentVolume:
        size: 30Gi
```

### Step 2: Export Cache

Export the cache volume contents:

```bash
kubectl cp stackgres/extensions-cache-pod:/var/cache/extensions ./extensions-backup
```

### Step 3: Import to Air-Gapped Environment

Import the cache to the isolated environment:

```bash
kubectl cp ./extensions-backup stackgres/extensions-cache-pod:/var/cache/extensions
```

## Cache with Custom Repository

When using a custom extensions repository:

```yaml
spec:
  extensions:
    repositoryUrls:
      - https://my-company.example.com/extensions/repository
    cache:
      enabled: true
      persistentVolume:
        size: 10Gi
```

The cache works with any configured repository.

## Best Practices

1. **Size appropriately**: Estimate cache size based on extensions used
   - PostGIS: ~500MB
   - TimescaleDB: ~300MB
   - Most extensions: 10-50MB

2. **Use persistent storage**: Always use PersistentVolume for production

3. **Pre-load common extensions**: Reduce initial deployment time

4. **Monitor disk usage**: Set up alerts for cache volume capacity

5. **Use fast storage**: SSD-backed storage improves performance

## Related Documentation

- [PostgreSQL Extensions Guide]({{% relref "04-administration-guide/07-postgres-extensions" %}})
- [Extension Versions]({{% relref "04-administration-guide/07-postgres-extensions/02-extension-versions" %}})
- [SGConfig Reference]({{% relref "06-crd-reference/12-sgconfig" %}})
