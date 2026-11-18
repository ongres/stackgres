---
title: Credentials Management
weight: 2
url: /administration/security/credentials
description: How to manage PostgreSQL credentials and secrets in StackGres.
showToc: true
---

StackGres automatically manages PostgreSQL credentials using Kubernetes Secrets. This guide covers how credentials are created, stored, and managed.

## Default Credentials

When you create an SGCluster, StackGres automatically generates credentials for the following PostgreSQL users:

| User | Purpose | Secret Key |
|------|---------|------------|
| `postgres` | Superuser | `superuser-password` |
| `replicator` | Replication | `replication-password` |
| `authenticator` | Patroni authentication | `authenticator-password` |

These credentials are stored in a Secret with the same name as the cluster.

## Retrieving Credentials

### Get the Superuser Password

```bash
# Get password for cluster named 'my-cluster'
kubectl get secret my-cluster \
  --template='{{ printf "%s" (index .data "superuser-password" | base64decode) }}'
```

### Get All Credentials

```bash
# List all credential keys
kubectl get secret my-cluster -o jsonpath='{.data}' | jq 'keys'

# Get all passwords
kubectl get secret my-cluster -o json | jq -r '.data | to_entries[] | "\(.key): \(.value | @base64d)"'
```

### Using Credentials in a Connection

```bash
# Set password in environment
export PGPASSWORD=$(kubectl get secret my-cluster \
  --template='{{ printf "%s" (index .data "superuser-password" | base64decode) }}')

# Connect
kubectl exec -it my-cluster-0 -c postgres-util -- psql -U postgres
```

## Custom Initial Passwords

You can pre-create the credentials Secret before creating the cluster to use your own passwords:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-cluster
type: Opaque
stringData:
  superuser-password: "MySecureSuperuserPassword123!"
  replication-password: "MySecureReplicationPassword123!"
  authenticator-password: "MySecureAuthenticatorPassword123!"
```

Apply the Secret before creating the cluster:

```bash
kubectl apply -f credentials-secret.yaml
kubectl apply -f sgcluster.yaml
```

## Password Rotation

### Manual Password Rotation

To rotate the superuser password:

```bash
# Generate new password
NEW_PASSWORD=$(openssl rand -base64 24)

# Update PostgreSQL
kubectl exec my-cluster-0 -c postgres-util -- psql -c \
  "ALTER USER postgres PASSWORD '$NEW_PASSWORD'"

# Update Secret
kubectl patch secret my-cluster -p \
  "{\"stringData\":{\"superuser-password\":\"$NEW_PASSWORD\"}}"
```

### Rotating All Passwords

```bash
#!/bin/bash
CLUSTER_NAME="my-cluster"

# Generate new passwords
SUPERUSER_PASS=$(openssl rand -base64 24)
REPLICATOR_PASS=$(openssl rand -base64 24)
AUTHENTICATOR_PASS=$(openssl rand -base64 24)

# Update PostgreSQL users
kubectl exec ${CLUSTER_NAME}-0 -c postgres-util -- psql << EOF
ALTER USER postgres PASSWORD '${SUPERUSER_PASS}';
ALTER USER replicator PASSWORD '${REPLICATOR_PASS}';
ALTER USER authenticator PASSWORD '${AUTHENTICATOR_PASS}';
EOF

# Update Secret
kubectl patch secret ${CLUSTER_NAME} -p "{
  \"stringData\": {
    \"superuser-password\": \"${SUPERUSER_PASS}\",
    \"replication-password\": \"${REPLICATOR_PASS}\",
    \"authenticator-password\": \"${AUTHENTICATOR_PASS}\"
  }
}"

echo "Passwords rotated successfully"
```

## Creating Application Users

### Using kubectl

```bash
# Create a new user
kubectl exec my-cluster-0 -c postgres-util -- psql << EOF
CREATE USER myapp WITH PASSWORD 'AppPassword123!';
CREATE DATABASE myappdb OWNER myapp;
GRANT ALL PRIVILEGES ON DATABASE myappdb TO myapp;
EOF
```

### Using SGScript

For managed, repeatable user creation, use [SGScript]({{% relref "04-administration-guide/15-sql-scripts" %}}):

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: create-app-user
spec:
  managedVersions: true
  scripts:
    - name: create-user
      script: |
        DO $$
        BEGIN
          IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'myapp') THEN
            CREATE USER myapp WITH PASSWORD 'AppPassword123!';
          END IF;
        END
        $$;
    - name: create-database
      script: |
        SELECT 'CREATE DATABASE myappdb OWNER myapp'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'myappdb');
```

### Storing Application Credentials

Create a separate Secret for application credentials:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: myapp-db-credentials
type: Opaque
stringData:
  username: myapp
  password: AppPassword123!
  database: myappdb
  host: my-cluster
  port: "5432"
  uri: postgresql://myapp:AppPassword123!@my-cluster:5432/myappdb
```

## External Secrets Management

For enhanced security, integrate with external secrets management systems.

### Using External Secrets Operator

[External Secrets Operator](https://external-secrets.io/) can sync secrets from AWS Secrets Manager, HashiCorp Vault, etc.

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: my-cluster
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: ClusterSecretStore
  target:
    name: my-cluster
    creationPolicy: Owner
  data:
    - secretKey: superuser-password
      remoteRef:
        key: postgres/my-cluster
        property: superuser-password
    - secretKey: replication-password
      remoteRef:
        key: postgres/my-cluster
        property: replication-password
    - secretKey: authenticator-password
      remoteRef:
        key: postgres/my-cluster
        property: authenticator-password
```

### Using Sealed Secrets

[Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) encrypts secrets for safe storage in Git:

```bash
# Seal the secret
kubeseal --format=yaml < credentials-secret.yaml > sealed-credentials.yaml
```

## Password Policies

While StackGres doesn't enforce password policies, you should implement them:

### Strong Password Generation

```bash
# Generate a strong password
openssl rand -base64 32

# Or using /dev/urandom
< /dev/urandom tr -dc 'A-Za-z0-9!@#$%^&*' | head -c 32
```

### Password Complexity Recommendations

- Minimum 16 characters
- Mix of uppercase, lowercase, numbers, and special characters
- Avoid dictionary words
- Use unique passwords for each user/environment

## Connecting Applications

### Environment Variables

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: myapp
spec:
  containers:
    - name: app
      image: myapp:latest
      env:
        - name: PGHOST
          value: my-cluster
        - name: PGPORT
          value: "5432"
        - name: PGDATABASE
          value: myappdb
        - name: PGUSER
          valueFrom:
            secretKeyRef:
              name: myapp-db-credentials
              key: username
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: myapp-db-credentials
              key: password
```

### Connection String from Secret

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: myapp
spec:
  containers:
    - name: app
      image: myapp:latest
      env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: myapp-db-credentials
              key: uri
```

## Security Best Practices

1. **Never commit secrets to Git** - Use Sealed Secrets or External Secrets Operator

2. **Limit secret access with RBAC**:
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: db-credentials-reader
rules:
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: ["myapp-db-credentials"]
    verbs: ["get"]
```

3. **Enable audit logging** for secret access

4. **Rotate credentials regularly** - Implement automated rotation

5. **Use separate credentials** for each application/environment

6. **Principle of least privilege** - Create users with minimal required permissions
