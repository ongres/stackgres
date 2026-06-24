---
title: Setting database credentials
weight: 10
url: /cookbook/operating-clusters/database-credentials
description: Provide or rotate the cluster superuser and internal user credentials.
showToc: true
---

## What it does

Points an [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) at Kubernetes Secrets
that hold the usernames and passwords for its internal Postgres users and the Patroni REST
API, via `spec.configurations.credentials`. When the field is absent the operator generates
credentials automatically; once you populate it the operator reads every value from the
Secrets you control.

## When to use it

- You need to supply credentials that comply with your organisation's password policy or
  secret-management tooling (for example Vault, External Secrets Operator).
- You are rotating a password and want the change to be driven by updating a Secret rather
  than by recreating the cluster.
- You are migrating an existing Postgres cluster into StackGres and must preserve the
  original `postgres` superuser password.

## How to do it

### 1. Create the Secrets

Each `username` and `password` field in the spec is a standard Kubernetes
`SecretKeySelector` — a `name` (Secret name) and a `key` (the entry inside that Secret).
You may use one Secret per credential or consolidate them.

```bash
# Superuser credentials
kubectl create secret generic pg-superuser-creds \
  --namespace my-cluster \
  --from-literal=username=postgres \
  --from-literal=password=change-me-superuser

# Replication user credentials
kubectl create secret generic pg-replication-creds \
  --namespace my-cluster \
  --from-literal=username=replicator \
  --from-literal=password=change-me-replication

# Authenticator user credentials (used by PgBouncer)
kubectl create secret generic pg-authenticator-creds \
  --namespace my-cluster \
  --from-literal=username=authenticator \
  --from-literal=password=change-me-authenticator

# Patroni REST API password
kubectl create secret generic patroni-api-creds \
  --namespace my-cluster \
  --from-literal=password=change-me-patroni-api
```

### 2. Reference them in the SGCluster

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
  pods:
    persistentVolume:
      size: 10Gi
  configurations:
    credentials:
      users:
        superuser:
          username:
            name: pg-superuser-creds   # Secret name
            key: username              # key inside the Secret
          password:
            name: pg-superuser-creds
            key: password
        replication:
          username:
            name: pg-replication-creds
            key: username
          password:
            name: pg-replication-creds
            key: password
        authenticator:
          username:
            name: pg-authenticator-creds
            key: username
          password:
            name: pg-authenticator-creds
            key: password
      patroni:
        restApiPassword:
          name: patroni-api-creds
          key: password
```

```bash
kubectl apply -f cluster.yaml
```

All four credential groups are optional and independent — configure only the ones you need.

## How it works

The operator reads the referenced Secret keys during each reconciliation cycle and injects
the values into the Patroni configuration and the Postgres `pg_hba.conf` / role
definitions. Because the spec stores only a pointer to the Secret (not the value itself),
updating the Secret's data and triggering a reconciliation is sufficient to propagate a new
password. The operator does **not** automatically execute `ALTER ROLE` on behalf of you —
for password changes the role already exists in Postgres so the new value takes effect on
the next connection; for username changes you must create the new role in the database
manually before applying the spec change (see Pitfalls).

## What to expect

- Verify that the cluster picked up the credentials reference:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.spec.configurations.credentials}'
  ```

- After applying a password rotation, check that Patroni restarted cleanly and that the
  cluster is healthy:

  ```bash
  kubectl get sgcluster -n my-cluster cluster -o jsonpath='{.status.conditions}'
  ```

- If the cluster enters a `pending-restart` condition, issue a rolling restart via
  `SGDbOps`:

  ```yaml
  apiVersion: stackgres.io/v1
  kind: SGDbOps
  metadata:
    namespace: my-cluster
    name: restart-after-creds
  spec:
    sgCluster: cluster
    op: restart
    restart:
      method: InPlace
  ```

  ```bash
  kubectl apply -f restart.yaml
  kubectl wait sgdbops -n my-cluster restart-after-creds \
    --for=condition=Completed --timeout=300s
  ```

## Pitfalls

- **Credential changes may require a restart.** Both `users` and `patroni.restApiPassword`
  are marked `may require restart` in the CRD. If the cluster enters a `pending-restart`
  condition after you apply a credential change, schedule an `SGDbOps` restart as shown
  above.
- **Username changes require manual DDL.** The operator updates its configuration but does
  not run `CREATE ROLE` or `ALTER ROLE ... RENAME` automatically. If you change a username,
  create the new role in Postgres before applying the spec change, otherwise the cluster
  will fail to authenticate. The required SQL is shown in the
  [SGCluster CRD reference]({{% relref "06-crd-reference/01-sgcluster" %}}).
- **The Secrets must exist before the cluster is reconciled.** If a referenced Secret is
  missing or the key is absent, the operator cannot read the credential and will report an
  error. Create all Secrets before applying or patching the `SGCluster`.
- **Omitting a credential group keeps the operator default.** Each sub-object
  (`superuser`, `replication`, `authenticator`, `patroni`) is individually optional. Fields
  you omit continue to use the operator-generated credential, so you can migrate to
  self-managed credentials one user at a time.
- **Do not store Secret values in the SGCluster spec.** The spec holds only
  `SecretKeySelector` references. Never inline a password string directly in the YAML; that
  would be rejected by the validating webhook and would bypass Secret-level RBAC.
