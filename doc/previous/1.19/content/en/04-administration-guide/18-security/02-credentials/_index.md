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
| `authenticator` | PgBouncer authentication | `authenticator-password` |
| `monitor` | Prometheus Postgres exporter | `monitor-password` |

The password of the Patroni REST API is also generated and stored with the key `restapi-password`.

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

The credentials of the users and of the Patroni REST API can be taken from your own Secrets with the
`spec.configurations.credentials` section of the SGCluster (see the
[SGCluster CRD reference]({{% relref "06-crd-reference/01-sgcluster" %}})). To rotate a password, change
it in the referenced Secret (or reference another Secret). At the next reconciliation cycle (usually
within a minute) the operator applies the new password to the database user and to Patroni, so no SQL has
to be run by hand and no restart is needed.

### Rotating All Passwords

Create a Secret with the new credentials:

```bash
#!/bin/bash
CLUSTER_NAME="my-cluster"

kubectl create secret generic "${CLUSTER_NAME}-credentials" \
  --from-literal=superuser-username=postgres \
  --from-literal=superuser-password="$(openssl rand -base64 24)" \
  --from-literal=replication-username=replicator \
  --from-literal=replication-password="$(openssl rand -base64 24)" \
  --from-literal=authenticator-username=authenticator \
  --from-literal=authenticator-password="$(openssl rand -base64 24)" \
  --from-literal=monitor-username=monitor \
  --from-literal=monitor-password="$(openssl rand -base64 24)" \
  --from-literal=restapi-password="$(openssl rand -base64 24)" \
  --dry-run=client -o yaml | kubectl apply -f -
```

Reference it in the SGCluster (this is needed only the first time):

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    credentials:
      patroni:
        restApiPassword:
          name: my-cluster-credentials
          key: restapi-password
      users:
        superuser:
          username:
            name: my-cluster-credentials
            key: superuser-username
          password:
            name: my-cluster-credentials
            key: superuser-password
        replication:
          username:
            name: my-cluster-credentials
            key: replication-username
          password:
            name: my-cluster-credentials
            key: replication-password
        authenticator:
          username:
            name: my-cluster-credentials
            key: authenticator-username
          password:
            name: my-cluster-credentials
            key: authenticator-password
        monitor:
          username:
            name: my-cluster-credentials
            key: monitor-username
          password:
            name: my-cluster-credentials
            key: monitor-password
```

### Changing Usernames

When the username of a user changes, the operator creates the user with the new username. The user with
the previous username is not removed. Patroni uses the new username of the superuser and of the replication
user once the user was created, PgBouncer uses the new username of the authenticator user to authenticate the
users that connect through it and the Postgres exporter uses the new username of the monitor user, without
restarting the Pods.

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
        CREATE EXTENSION IF NOT EXISTS dblink;
        DO $$
        BEGIN
          IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'myappdb') THEN
            PERFORM dblink_exec('', 'CREATE DATABASE myappdb OWNER myapp');
          END IF;
        END
        $$;
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
