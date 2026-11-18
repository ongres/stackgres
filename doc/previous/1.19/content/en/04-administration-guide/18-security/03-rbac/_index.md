---
title: RBAC and Access Control
weight: 3
url: /administration/security/rbac
description: Kubernetes RBAC configuration for StackGres operator and clusters.
showToc: true
---

This guide covers Kubernetes Role-Based Access Control (RBAC) configuration for StackGres, including operator permissions and user access control.

## Operator RBAC

The StackGres operator requires certain Kubernetes permissions to manage PostgreSQL clusters.

### Default Operator Permissions

By default, the operator uses a ClusterRole with permissions to:

- Manage StackGres CRDs (SGCluster, SGBackup, etc.)
- Create and manage Pods, StatefulSets, Services
- Manage Secrets and ConfigMaps
- Watch cluster events

### Namespace-Scoped Installation

For enhanced security, you can limit the operator to specific namespaces:

```bash
# During Helm installation
helm install stackgres-operator stackgres-charts/stackgres-operator \
  --set cluster.create=false \
  --set allowedNamespaces='{namespace1,namespace2}' \
  --set disableClusterRole=true
```

Or using SGConfig:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  allowedNamespaces:
    - production
    - staging
  disableClusterRole: true
```

### Allowed Namespaces by Label

Select namespaces by label instead of explicit list:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  allowedNamespaceLabelSelector:
    stackgres.io/enabled: "true"
```

Then label namespaces:

```bash
kubectl label namespace production stackgres.io/enabled=true
```

## User Access Control

### Admin Access

Full access to all StackGres resources:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: stackgres-admin
rules:
  - apiGroups: ["stackgres.io"]
    resources: ["*"]
    verbs: ["*"]
  - apiGroups: [""]
    resources: ["secrets"]
    verbs: ["get", "list", "create", "update", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: stackgres-admin-binding
subjects:
  - kind: User
    name: admin@example.com
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: stackgres-admin
  apiGroup: rbac.authorization.k8s.io
```

### Developer Access (Read-Only)

View clusters but not modify or access secrets:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: stackgres-viewer
rules:
  - apiGroups: ["stackgres.io"]
    resources:
      - sgclusters
      - sgbackups
      - sgdbops
      - sgpgconfigs
      - sgpoolconfigs
      - sginstanceprofiles
      - sgobjectstorages
      - sgscripts
    verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: stackgres-viewer-binding
subjects:
  - kind: Group
    name: developers
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: stackgres-viewer
  apiGroup: rbac.authorization.k8s.io
```

### Namespace-Scoped Access

Limit access to specific namespaces:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: stackgres-team-admin
  namespace: team-a
rules:
  - apiGroups: ["stackgres.io"]
    resources: ["*"]
    verbs: ["*"]
  - apiGroups: [""]
    resources: ["secrets"]
    verbs: ["get", "list", "create", "update", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: stackgres-team-admin-binding
  namespace: team-a
subjects:
  - kind: Group
    name: team-a-admins
    apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role
  name: stackgres-team-admin
  apiGroup: rbac.authorization.k8s.io
```

### Backup Operator Role

Allow managing backups only:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: stackgres-backup-operator
rules:
  - apiGroups: ["stackgres.io"]
    resources:
      - sgbackups
      - sgobjectstorages
    verbs: ["get", "list", "watch", "create", "delete"]
  - apiGroups: ["stackgres.io"]
    resources:
      - sgclusters
    verbs: ["get", "list"]
```

### DBA Role

Manage configurations and perform operations:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: stackgres-dba
rules:
  - apiGroups: ["stackgres.io"]
    resources:
      - sgclusters
      - sgpgconfigs
      - sgpoolconfigs
      - sginstanceprofiles
      - sgscripts
    verbs: ["get", "list", "watch", "update", "patch"]
  - apiGroups: ["stackgres.io"]
    resources:
      - sgdbops
      - sgbackups
    verbs: ["*"]
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: []  # Restrict to specific secrets if needed
    verbs: ["get"]
```

## Web Console Access Control

### Authentication Configuration

The StackGres Web Console supports multiple authentication methods.

#### JWT Authentication (Default)

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
spec:
  authentication:
    type: jwt
```

#### OIDC Authentication

Integrate with identity providers like Keycloak, Okta, or Azure AD:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
spec:
  authentication:
    type: oidc
    oidc:
      clientId: stackgres
      clientIdSecretRef:
        name: oidc-secret
        key: client-secret
      authServerUrl: https://keycloak.example.com/realms/stackgres
```

### Console Admin User

Configure the Web Console admin user:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
spec:
  authentication:
    user: admin
    secretRef:
      name: stackgres-admin-secret # Make sure the `user` field match the value of the `k8sUsername` key in the referenced Secret.
```

## Service Account for Applications

Create a service account for applications that need to interact with StackGres:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: app-database-access
  namespace: production
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: app-db-credentials
  namespace: production
rules:
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: ["myapp-db-credentials"]
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: app-db-credentials-binding
  namespace: production
subjects:
  - kind: ServiceAccount
    name: app-database-access
    namespace: production
roleRef:
  kind: Role
  name: app-db-credentials
  apiGroup: rbac.authorization.k8s.io
```

## Audit Logging

Enable Kubernetes audit logging to track access to StackGres resources:

```yaml
# Example audit policy
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
  # Log all access to secrets at metadata level
  - level: Metadata
    resources:
      - group: ""
        resources: ["secrets"]
  # Log all StackGres resource access at request level
  - level: Request
    resources:
      - group: "stackgres.io"
        resources: ["*"]
```

## Best Practices

1. **Principle of Least Privilege**: Grant only the permissions needed for each role

2. **Namespace Isolation**: Use namespaces to separate environments and teams

3. **Separate Credentials Access**: Create separate roles for viewing clusters vs. accessing credentials

4. **Regular Audits**: Review RBAC bindings regularly

5. **Use Groups**: Bind roles to groups rather than individual users when possible

6. **Document Access**: Maintain documentation of who has access to what
