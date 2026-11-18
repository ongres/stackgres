---
title: Deprecated Field Migration
weight: 1
url: /administration/upgrade/deprecated-fields
description: How to migrate from deprecated fields to their replacements in StackGres.
showToc: true
---

This guide helps you migrate from deprecated configuration fields to their current replacements. Deprecated fields continue to work but will be removed in future versions.

## Overview

StackGres occasionally deprecates fields when better alternatives are introduced. This ensures:
- Cleaner API design
- Better separation of concerns
- Improved configurability

> **Recommendation**: Migrate to new fields as soon as possible to avoid issues during future upgrades.

## Deprecated Fields Reference

| Deprecated Field | Replacement | Removed In |
|-----------------|-------------|------------|
| `spec.pods.disableMetricsExporter` | `spec.configurations.observability.disableMetrics` | Future |
| `spec.initialData.scripts` | `spec.managedSql` with SGScript | Future |
| `spec.initialData.restore.fromBackup.uid` | `spec.initialData.restore.fromBackup.name` | Future |

## disableMetricsExporter Migration

The `spec.pods.disableMetricsExporter` field has been moved to the observability configuration section for better organization.

### Before (Deprecated)

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
    disableMetricsExporter: true  # DEPRECATED
```

### After (Current)

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
  configurations:
    observability:
      disableMetrics: true  # New location
```

### Migration Steps

1. **Identify clusters using deprecated field**:
   ```bash
   kubectl get sgcluster -A -o yaml | grep -B20 "disableMetricsExporter: true"
   ```

2. **Update cluster spec**:
   ```bash
   kubectl edit sgcluster my-cluster
   ```

   Remove `spec.pods.disableMetricsExporter` and add `spec.configurations.observability.disableMetrics`.

3. **Verify configuration**:
   ```bash
   kubectl get sgcluster my-cluster -o jsonpath='{.spec.configurations.observability}'
   ```

### Additional Observability Options

The new location provides more observability settings:

```yaml
configurations:
  observability:
    disableMetrics: true
    prometheusAutobind: false
    receiver: my-otel-receiver  # OpenTelemetry Collector
```

## initialData.scripts Migration

The `spec.initialData.scripts` field has been replaced by the more powerful `managedSql` system with SGScript resources.

### Before (Deprecated)

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
  initialData:
    scripts:  # DEPRECATED
      - name: create-database
        script: |
          CREATE DATABASE myapp;
      - name: create-user
        scriptFrom:
          secretKeyRef:
            name: db-credentials
            key: create-user.sql
```

### After (Current)

**Step 1**: Create an SGScript resource:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: my-cluster-init
spec:
  scripts:
    - name: create-database
      script: |
        CREATE DATABASE myapp;
    - name: create-user
      scriptFrom:
        secretKeyRef:
          name: db-credentials
          key: create-user.sql
```

**Step 2**: Reference the SGScript in the cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  instances: 3
  postgres:
    version: '16'
  pods:
    persistentVolume:
      size: '50Gi'
  managedSql:
    scripts:
      - sgScript: my-cluster-init
```

### Migration Steps

1. **Export existing scripts**:
   ```bash
   kubectl get sgcluster my-cluster -o jsonpath='{.spec.initialData.scripts}' > scripts.json
   ```

2. **Create SGScript resource**:
   ```yaml
   apiVersion: stackgres.io/v1
   kind: SGScript
   metadata:
     name: my-cluster-init
   spec:
     scripts:
       # Convert your scripts here
   ```

3. **Apply SGScript**:
   ```bash
   kubectl apply -f sgscript.yaml
   ```

4. **Update cluster to use managedSql**:
   ```bash
   kubectl patch sgcluster my-cluster --type=merge -p '
   spec:
     managedSql:
       scripts:
         - sgScript: my-cluster-init
   '
   ```

5. **Remove deprecated field** (after verifying scripts work):
   ```bash
   kubectl patch sgcluster my-cluster --type=json -p '[
     {"op": "remove", "path": "/spec/initialData/scripts"}
   ]'
   ```

### Benefits of managedSql

The new `managedSql` system provides:

- **Reusability**: SGScripts can be shared across clusters
- **Versioning**: Scripts can be versioned and re-executed
- **Status tracking**: Execution status visible in cluster status
- **Error handling**: `continueOnSGScriptError` and `continueOnError` options
- **Ordering control**: Fine-grained execution order

### Script Status Tracking

With `managedSql`, you can track script execution:

```bash
kubectl get sgcluster my-cluster -o jsonpath='{.status.managedSql}' | jq
```

Example output:
```json
{
  "scripts": [
    {
      "id": 0,
      "startedAt": "2024-01-15T10:00:00Z",
      "completedAt": "2024-01-15T10:00:05Z",
      "scripts": [
        {"id": 0, "version": 1},
        {"id": 1, "version": 1}
      ]
    }
  ]
}
```

## fromBackup.uid Migration

The `spec.initialData.restore.fromBackup.uid` field is deprecated in favor of `name`.

### Before (Deprecated)

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: restored-cluster
spec:
  initialData:
    restore:
      fromBackup:
        uid: a1b2c3d4-e5f6-7890-abcd-ef1234567890  # DEPRECATED
```

### After (Current)

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: restored-cluster
spec:
  initialData:
    restore:
      fromBackup:
        name: my-backup  # Use backup name instead
```

### Migration Steps

1. **Find backup name from UID**:
   ```bash
   kubectl get sgbackup -A -o custom-columns='NAME:.metadata.name,UID:.metadata.uid'
   ```

2. **Update cluster spec** to use `name` instead of `uid`.

## Checking for Deprecated Fields

### Audit Script

Check all clusters for deprecated fields:

```bash
#!/bin/bash
echo "Checking for deprecated fields..."

for cluster in $(kubectl get sgcluster -A -o jsonpath='{range .items[*]}{.metadata.namespace}/{.metadata.name}{"\n"}{end}'); do
  ns=$(echo $cluster | cut -d'/' -f1)
  name=$(echo $cluster | cut -d'/' -f2)

  # Check disableMetricsExporter
  if kubectl get sgcluster -n $ns $name -o jsonpath='{.spec.pods.disableMetricsExporter}' 2>/dev/null | grep -q "true"; then
    echo "[$ns/$name] Uses deprecated: spec.pods.disableMetricsExporter"
  fi

  # Check initialData.scripts
  if kubectl get sgcluster -n $ns $name -o jsonpath='{.spec.initialData.scripts}' 2>/dev/null | grep -q "."; then
    echo "[$ns/$name] Uses deprecated: spec.initialData.scripts"
  fi

  # Check fromBackup.uid
  if kubectl get sgcluster -n $ns $name -o jsonpath='{.spec.initialData.restore.fromBackup.uid}' 2>/dev/null | grep -q "."; then
    echo "[$ns/$name] Uses deprecated: spec.initialData.restore.fromBackup.uid"
  fi
done
```

### Warnings in Logs

The operator logs warnings when deprecated fields are used:

```bash
kubectl logs -n stackgres -l app=stackgres-operator | grep -i deprecated
```

## Best Practices

1. **Test migrations in non-production** before applying to production clusters

2. **Keep both fields temporarily** during migration if supported

3. **Document changes** in your GitOps repositories

4. **Monitor after migration** to ensure functionality is preserved

5. **Update automation** scripts and Helm values that use deprecated fields

## Related Documentation

- [Managed SQL Scripts]({{% relref "04-administration-guide/15-sql-scripts" %}})
- [SGScript Reference]({{% relref "06-crd-reference/10-sgscript" %}})
- [Container Configuration]({{% relref "04-administration-guide/04-configuration/04-container-configuration" %}})
- [Monitoring]({{% relref "04-administration-guide/08-monitoring" %}})
