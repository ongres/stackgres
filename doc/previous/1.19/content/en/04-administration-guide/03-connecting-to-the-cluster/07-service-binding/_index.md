---
title: Service Binding
weight: 7
url: /administration/cluster/connection/service-binding
description: How to use Service Binding spec to connect applications to StackGres clusters.
showToc: true
---

StackGres implements the [Service Binding specification](https://servicebinding.io/) for provisioned services, making it easy to connect applications to PostgreSQL clusters using a standardized approach.

## What is Service Binding?

Service Binding is a Kubernetes specification that standardizes how applications discover and connect to backing services like databases. Instead of manually configuring connection details, applications can automatically bind to services that implement the specification.

Key benefits:
- **Standardized**: Works with any Service Binding-compliant application framework
- **Automatic Discovery**: Connection details are automatically projected into application pods
- **Secure**: Credentials are managed through Kubernetes Secrets
- **Portable**: Same approach works across different service providers

## How StackGres Implements Service Binding

When you create an SGCluster, StackGres automatically creates a binding Secret that conforms to the [Service Binding specification](https://servicebinding.io/spec/core/1.0.0/#provisioned-service).

The binding Secret is referenced in the cluster's status:

```yaml
status:
  binding:
    name: my-cluster-binding
```

## Binding Secret Contents

The binding Secret contains all connection information:

| Key | Description | Example |
|-----|-------------|---------|
| `type` | Service type | `postgresql` |
| `provider` | Provider name | `stackgres` |
| `host` | Database hostname | `my-cluster` |
| `port` | Database port | `5432` |
| `database` | Database name | `postgres` |
| `username` | Username | `postgres` |
| `password` | Password | `<password>` |
| `uri` | Connection URI | `postgresql://postgres:pass@my-cluster:5432/postgres` |

## Using Service Binding

### With Service Binding Operator

The [Service Binding Operator](https://github.com/servicebinding/service-binding-controller) automatically projects binding information into your application pods.

#### 1. Install Service Binding Operator

```bash
# Using OperatorHub (OpenShift)
# Or install manually
kubectl apply -f https://github.com/servicebinding/service-binding-controller/releases/latest/download/service-binding-controller.yaml
```

#### 2. Create a ServiceBinding Resource

```yaml
apiVersion: servicebinding.io/v1beta1
kind: ServiceBinding
metadata:
  name: myapp-postgres-binding
spec:
  service:
    apiVersion: stackgres.io/v1
    kind: SGCluster
    name: my-cluster
  workload:
    apiVersion: apps/v1
    kind: Deployment
    name: myapp
```

#### 3. Deploy Your Application

The operator will automatically inject binding information as files in `/bindings/<binding-name>/`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    spec:
      containers:
        - name: app
          image: myapp:latest
          # Bindings are automatically mounted at /bindings/myapp-postgres-binding/
```

Your application can read connection details from files:
- `/bindings/myapp-postgres-binding/host`
- `/bindings/myapp-postgres-binding/port`
- `/bindings/myapp-postgres-binding/username`
- `/bindings/myapp-postgres-binding/password`
- `/bindings/myapp-postgres-binding/database`
- `/bindings/myapp-postgres-binding/uri`

### Manual Binding (Without Operator)

You can manually project the binding Secret into your application:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    spec:
      containers:
        - name: app
          image: myapp:latest
          env:
            - name: SERVICE_BINDING_ROOT
              value: /bindings
          volumeMounts:
            - name: postgres-binding
              mountPath: /bindings/postgres
              readOnly: true
      volumes:
        - name: postgres-binding
          secret:
            secretName: my-cluster-binding
```

Or as environment variables:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  template:
    spec:
      containers:
        - name: app
          image: myapp:latest
          env:
            - name: DATABASE_HOST
              valueFrom:
                secretKeyRef:
                  name: my-cluster-binding
                  key: host
            - name: DATABASE_PORT
              valueFrom:
                secretKeyRef:
                  name: my-cluster-binding
                  key: port
            - name: DATABASE_NAME
              valueFrom:
                secretKeyRef:
                  name: my-cluster-binding
                  key: database
            - name: DATABASE_USER
              valueFrom:
                secretKeyRef:
                  name: my-cluster-binding
                  key: username
            - name: DATABASE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: my-cluster-binding
                  key: password
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: my-cluster-binding
                  key: uri
```

## Customizing the Binding

You can customize the binding configuration in the SGCluster spec:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    binding:
      provider: my-company        # Custom provider name (default: stackgres)
      database: myappdb           # Specific database (default: postgres)
      username: myapp             # Specific username (default: superuser)
      password:                   # Custom password secret
        name: myapp-credentials
        key: password
```

### Using a Specific Database User

To bind with a non-superuser account:

1. Create the user and database:
```bash
kubectl exec my-cluster-0 -c postgres-util -- psql << EOF
CREATE USER myapp WITH PASSWORD 'secure_password';
CREATE DATABASE myappdb OWNER myapp;
EOF
```

2. Store credentials in a Secret:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: myapp-credentials
type: Opaque
stringData:
  password: secure_password
```

3. Configure the binding:
```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    binding:
      database: myappdb
      username: myapp
      password:
        name: myapp-credentials
        key: password
```

## Framework Integration

Many application frameworks support Service Binding natively.

### Spring Boot

Spring Cloud Bindings automatically detects PostgreSQL bindings:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-bindings</artifactId>
</dependency>
```

No additional configuration needed - Spring Boot will automatically configure the DataSource.

### Quarkus

Quarkus supports Service Binding via the `quarkus-kubernetes-service-binding` extension:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes-service-binding</artifactId>
</dependency>
```

### Node.js

Use the `kube-service-bindings` library:

```javascript
const { getBinding } = require('kube-service-bindings');

const binding = getBinding('POSTGRESQL');
const connectionString = binding.uri;
```

### Python

Read bindings from the file system:

```python
import os

binding_root = os.environ.get('SERVICE_BINDING_ROOT', '/bindings')
binding_name = 'postgres'

def read_binding(key):
    with open(f'{binding_root}/{binding_name}/{key}') as f:
        return f.read().strip()

host = read_binding('host')
port = read_binding('port')
database = read_binding('database')
username = read_binding('username')
password = read_binding('password')

connection_string = f"postgresql://{username}:{password}@{host}:{port}/{database}"
```

## Checking the Binding Secret

View the binding information:

```bash
# Get the binding secret name from cluster status
kubectl get sgcluster my-cluster -o jsonpath='{.status.binding.name}'

# View binding contents
kubectl get secret my-cluster-binding -o json | jq -r '.data | to_entries[] | "\(.key): \(.value | @base64d)"'
```

## Multiple Bindings

For applications that need different access levels, create multiple SGClusters or use custom bindings:

```yaml
# Read-write binding (default)
apiVersion: servicebinding.io/v1beta1
kind: ServiceBinding
metadata:
  name: myapp-postgres-rw
spec:
  service:
    apiVersion: stackgres.io/v1
    kind: SGCluster
    name: my-cluster
  workload:
    apiVersion: apps/v1
    kind: Deployment
    name: myapp
---
# Read-only binding (using replicas service)
apiVersion: v1
kind: Secret
metadata:
  name: my-cluster-readonly-binding
type: servicebinding.io/postgresql
stringData:
  type: postgresql
  provider: stackgres
  host: my-cluster-replicas
  port: "5432"
  database: postgres
  username: readonly_user
  password: readonly_password
```

## Related Documentation

- [Service Binding Specification](https://servicebinding.io/)
- [Passwords]({{% relref "04-administration-guide/03-connecting-to-the-cluster/01-passwords" %}})
- [DNS Configuration]({{% relref "04-administration-guide/03-connecting-to-the-cluster/02-dns" %}})
