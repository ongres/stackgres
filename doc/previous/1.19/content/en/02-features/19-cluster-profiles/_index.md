---
title: Cluster Profiles
weight: 19
url: /features/cluster-profiles
description: Pre-configured cluster profiles for production, testing, and development environments.
---

StackGres provides cluster profiles that adjust default configuration settings based on the intended environment. Profiles offer a convenient way to set sensible defaults for pod anti-affinity and resource requirements without having to configure each option individually.

## Available Profiles

The `SGCluster.spec.profile` field accepts one of three values:

| Profile | Pod Anti-Affinity | Patroni Resource Requirements | Sidecar Resource Requirements |
|---------|-------------------|-------------------------------|-------------------------------|
| `production` (default) | Enabled - prevents two Pods from running on the same Node | Enabled - sets both limits and requests from the SGInstanceProfile | Enabled - sets requests from the SGInstanceProfile |
| `testing` | Disabled - allows two Pods on the same Node | Enabled - sets both limits and requests from the SGInstanceProfile | Enabled - sets requests from the SGInstanceProfile |
| `development` | Disabled - allows two Pods on the same Node | Disabled - unsets limits and requests for the patroni container | Disabled - unsets requests for sidecar containers |

## Usage

Set the profile in your SGCluster definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  profile: production
  instances: 3
  postgres:
    version: '16'
  sgInstanceProfile: size-small
```

## Profile Details

### Production

The default profile. Enforces resource isolation and high availability best practices:

- **Pod anti-affinity** is enabled, ensuring that cluster Pods are scheduled on different Kubernetes Nodes. This prevents a single Node failure from taking down multiple instances.
- **Resource requirements** are enforced for all containers using the referenced [SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}), ensuring predictable performance and proper Kubernetes scheduling.

### Testing

Relaxes scheduling constraints while maintaining resource requirements:

- **Pod anti-affinity** is disabled, allowing multiple cluster Pods to run on the same Node. This is useful for testing environments with limited infrastructure.
- **Resource requirements** remain enforced, matching production resource behavior.

### Development

Removes most constraints for lightweight local development:

- **Pod anti-affinity** is disabled.
- **Resource requirements** are unset for all containers, allowing Pods to run without CPU or memory limits. This is useful when running on resource-constrained development machines.

## Underlying Configuration

Each profile sets defaults for fields under `SGCluster.spec.nonProductionOptions`:

| Field | production | testing | development |
|-------|-----------|---------|-------------|
| `disableClusterPodAntiAffinity` | `false` | `true` | `true` |
| `disablePatroniResourceRequirements` | `false` | `false` | `true` |
| `disableClusterResourceRequirements` | `false` | `false` | `true` |

These fields can still be overridden individually if you need a custom combination. The profile simply provides convenient defaults.

> Changing the profile field may require a restart of the cluster Pods.

## Related Documentation

- [SGCluster CRD Reference]({{% relref "06-crd-reference/01-sgcluster" %}})
- [Instance Profile Configuration]({{% relref "04-administration-guide/04-configuration/01-instance-profile" %}})
