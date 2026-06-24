---
title: Enabling and configuring TLS/SSL
weight: 14
url: /cookbook/operating-clusters/tls-ssl
description: Configure SSL/TLS for client connections to the cluster.
showToc: true
---

## What it does

Controls the `spec.postgres.ssl` section of an
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) to enable or disable SSL for
client connections to Postgres and, optionally, to supply a custom certificate and private
key from a Kubernetes Secret. When no certificate is provided, the operator generates a
self-signed one automatically.

## When to use it

- You need to encrypt client-to-Postgres traffic, for example to satisfy compliance
  requirements or to protect credentials in transit on a shared network.
- You want to supply a certificate signed by your internal or public CA instead of the
  operator-generated self-signed certificate.
- You need to control the lifetime of the auto-generated certificate via the `duration`
  field.
- SSL is enabled by default (`enabled: true`); use this recipe if you want to disable it,
  change the duration, or bring your own certificate.

## How to do it

### Option A: Use the auto-generated self-signed certificate

SSL is on by default; the minimal change to be explicit about it is:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: "16"
    ssl:
      enabled: true           # default; shown here for clarity
      duration: P365DT0H0M0S  # ISO 8601: auto-generated cert valid for 1 year
  pods:
    persistentVolume:
      size: 10Gi
```

```bash
kubectl apply -f cluster.yaml
```

`duration` is an ISO 8601 duration (`PnDTnHnMn.nS`). Omitting it gives the default of
13 months. The certificate is renewed 1 day before expiry, or 1/12th of the duration,
whichever is greater.

### Option B: Provide your own certificate and key

Store the PEM-encoded certificate (or chain) and private key in a Secret:

```bash
kubectl create secret generic cluster-tls \
  --namespace my-cluster \
  --from-file=tls.crt=server.crt \
  --from-file=tls.key=server.key
```

Then reference the Secret from the `SGCluster`:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: "16"
    ssl:
      enabled: true
      certificateSecretKeySelector:
        name: cluster-tls   # Secret name
        key: tls.crt        # key inside the Secret that holds the certificate
      privateKeySecretKeySelector:
        name: cluster-tls   # Secret name (may be the same or different)
        key: tls.key        # key inside the Secret that holds the private key
  pods:
    persistentVolume:
      size: 10Gi
```

```bash
kubectl apply -f cluster.yaml
```

Both selectors (`certificateSecretKeySelector` and `privateKeySecretKeySelector`) must be
supplied together; omitting either one reverts to auto-generation.

## How it works

`spec.postgres.ssl` is reconciled by the operator on every sync cycle. When `enabled` is
`true` and no selectors are given, the operator creates and manages a self-signed
certificate and injects it into each Postgres instance via `postgresql.conf`
(`ssl_cert_file`, `ssl_key_file`). When selectors are provided, the operator reads those
Secret keys and uses them instead. Certificate rotation (for auto-generated certs) happens
in-place without a cluster restart; Postgres reloads SSL configuration via `pg_reload_conf`.

Setting `enabled: false` removes the `ssl = on` directive from `postgresql.conf` and
Postgres stops accepting SSL connections.

## What to expect

Verify that SSL is active by connecting with `psql` and checking the connection info:

```bash
kubectl exec -n my-cluster cluster-0 -c patroni -- \
  psql -U postgres -c "\conninfo"
```

The output should include `SSL connection (protocol: TLSv1.3 ...)`.

Check the current certificate expiry:

```bash
kubectl exec -n my-cluster cluster-0 -c patroni -- \
  psql -U postgres -c "SELECT * FROM pg_stat_ssl WHERE pid = pg_backend_pid();"
```

## Pitfalls

- **Auto-generated certificates expire.** The default lifetime is 13 months. The operator
  renews them automatically, but if the operator is unavailable near the expiry window the
  certificate may lapse and clients will refuse to connect. Set a longer `duration` or
  monitor certificate expiry externally.
- **Self-signed certificates are not trusted by clients by default.** Connect with
  `sslmode=require` (encrypts but skips verification) or add the operator-managed CA cert
  to the client trust store to use `sslmode=verify-ca` / `verify-full`.
- **Both selectors must be set together.** Providing only one of
  `certificateSecretKeySelector` or `privateKeySecretKeySelector` will cause the operator to
  ignore the partial configuration and fall back to auto-generation.
- **Secret must exist before reconciliation.** If the referenced Secret is missing or the
  specified key is absent, the operator will log an error and the SSL configuration will
  not be applied until the Secret is created.
- **Disabling SSL affects all clients.** Setting `enabled: false` removes SSL from all
  connections immediately after the next reload; ensure no clients require SSL before
  making this change.
