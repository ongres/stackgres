---
title: SSL/TLS Configuration
weight: 1
url: /administration/security/ssl-tls
description: How to configure SSL/TLS encryption for PostgreSQL connections.
showToc: true
---

StackGres enables SSL/TLS encryption by default for all PostgreSQL connections, protecting data in transit between clients and the database.

## Default Behavior

By default, StackGres:

- **Enables SSL/TLS** for all PostgreSQL connections
- **Auto-generates certificates** with a 13-month validity period
- **Auto-renews certificates** before expiration (1 day or 1/12th of duration before expiry)

No configuration is required for basic SSL/TLS protection.

## SSL Configuration Options

### Disable SSL (Not Recommended)

To disable SSL (not recommended for production):

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  postgres:
    ssl:
      enabled: false
```

### Custom Certificate Duration

Change the auto-generated certificate duration using ISO 8601 format:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  postgres:
    ssl:
      enabled: true
      duration: P365D  # 365 days
```

Duration format examples:
- `P30D` - 30 days
- `P6M` - 6 months
- `P1Y` - 1 year
- `P1Y6M` - 1 year and 6 months

### Using Custom Certificates

For production environments, you may want to use certificates from your organization's PKI or a trusted CA.

#### Step 1: Create Certificate and Key

Generate or obtain your certificate and private key. For example, using OpenSSL:

```bash
# Generate private key
openssl genrsa -out server.key 4096

# Generate certificate signing request
openssl req -new -key server.key -out server.csr \
  -subj "/CN=my-cluster.default.svc.cluster.local"

# Self-sign the certificate (or submit CSR to your CA)
openssl x509 -req -in server.csr -signkey server.key \
  -out server.crt -days 365
```

For proper hostname verification, include Subject Alternative Names:

```bash
# Create a config file for SANs
cat > san.cnf <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req

[req_distinguished_name]
CN = my-cluster

[v3_req]
subjectAltName = @alt_names

[alt_names]
DNS.1 = my-cluster
DNS.2 = my-cluster.default
DNS.3 = my-cluster.default.svc
DNS.4 = my-cluster.default.svc.cluster.local
DNS.5 = my-cluster-primary
DNS.6 = my-cluster-replicas
EOF

# Generate with SANs
openssl req -new -key server.key -out server.csr -config san.cnf
openssl x509 -req -in server.csr -signkey server.key \
  -out server.crt -days 365 -extensions v3_req -extfile san.cnf
```

#### Step 2: Create Kubernetes Secret

```bash
kubectl create secret generic my-cluster-ssl \
  --from-file=tls.crt=server.crt \
  --from-file=tls.key=server.key
```

Or using a YAML manifest:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: my-cluster-ssl
type: Opaque
data:
  tls.crt: <base64-encoded-certificate>
  tls.key: <base64-encoded-private-key>
```

#### Step 3: Configure SGCluster

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  postgres:
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: my-cluster-ssl
        key: tls.crt
      privateKeySecretKeySelector:
        name: my-cluster-ssl
        key: tls.key
```

## Using cert-manager

For automated certificate management, integrate with [cert-manager](https://cert-manager.io/):

### Step 1: Create a Certificate Resource

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: my-cluster-cert
spec:
  secretName: my-cluster-ssl
  duration: 8760h  # 1 year
  renewBefore: 720h  # 30 days
  subject:
    organizations:
      - my-organization
  commonName: my-cluster
  dnsNames:
    - my-cluster
    - my-cluster.default
    - my-cluster.default.svc
    - my-cluster.default.svc.cluster.local
    - my-cluster-primary
    - my-cluster-replicas
  issuerRef:
    name: my-issuer
    kind: ClusterIssuer
```

### Step 2: Reference in SGCluster

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  postgres:
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: my-cluster-ssl
        key: tls.crt
      privateKeySecretKeySelector:
        name: my-cluster-ssl
        key: tls.key
```

cert-manager will automatically renew the certificate before expiration.

## Client SSL Connection

### Verify SSL is Enabled

Connect and check the connection:

```bash
kubectl exec -it my-cluster-0 -c postgres-util -- psql -c "SHOW ssl"
```

### Connection String with SSL

```bash
# Require SSL
psql "host=my-cluster port=5432 dbname=postgres user=postgres sslmode=require"

# Verify server certificate
psql "host=my-cluster port=5432 dbname=postgres user=postgres sslmode=verify-full sslrootcert=/path/to/ca.crt"
```

### SSL Modes

| Mode | Description |
|------|-------------|
| `disable` | No SSL |
| `allow` | Try non-SSL, then SSL |
| `prefer` | Try SSL, then non-SSL (default) |
| `require` | Require SSL, don't verify certificate |
| `verify-ca` | Require SSL, verify CA |
| `verify-full` | Require SSL, verify CA and hostname |

For production, use `verify-full` when possible.

## Retrieving Auto-Generated Certificates

If using auto-generated certificates and you need the CA for client verification:

```bash
# Get the certificate from the cluster
kubectl exec my-cluster-0 -c postgres-util -- \
  cat /etc/ssl/server.crt > server.crt
```

## Certificate Rotation

### Auto-Generated Certificates

Auto-generated certificates are automatically rotated before expiration. PostgreSQL will reload the new certificates without restart.

### Custom Certificates

For custom certificates:

1. Update the Secret with the new certificate and key
2. The operator will detect the change and reload PostgreSQL configuration

```bash
# Update the secret
kubectl create secret generic my-cluster-ssl \
  --from-file=tls.crt=new-server.crt \
  --from-file=tls.key=new-server.key \
  --dry-run=client -o yaml | kubectl apply -f -
```
