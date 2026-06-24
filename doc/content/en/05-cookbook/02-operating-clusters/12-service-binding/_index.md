---
title: Service binding for applications
weight: 12
url: /cookbook/operating-clusters/service-binding
description: Expose connection details to applications via Service Binding.
showToc: true
---

## What it does

Configures the `spec.configurations.binding` section of a running
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) so that the operator
populates a Kubernetes Secret whose name is recorded in
`status.binding.name`. Any Service Binding-aware runtime can then project that
Secret into application Pods as environment variables or volume files, following
the [Service Binding Specification for Kubernetes](https://servicebinding.io/spec/core/1.0.0/).

## When to use it

- You run a Service Binding-aware runtime (for example the Service Binding Operator
  or a Quarkus application using the Quarkus Kubernetes Service Binding extension)
  and want it to discover connection details automatically.
- You want to bind a specific database or application-level user — rather than the
  superuser — to an application.
- You need a standardised, operator-maintained credential Secret that does not
  require manual updates when passwords rotate.

## How to do it

Patch (or update) the `SGCluster` to add a `configurations.binding` section:

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
    binding:
      # Database that consuming applications connect to.
      # Defaults to "postgres" when omitted.
      database: myapp

      # Application-level Postgres username.
      # Defaults to the superuser when omitted.
      username: myapp_user

      # Reference to an existing Secret that holds the password for the user above.
      # Defaults to the superuser password Secret when omitted.
      password:
        name: myapp-user-secret   # Secret name in the same namespace
        key: password             # key inside that Secret

      # Arbitrary provider label written into the binding Secret.
      # Defaults to "stackgres" when omitted.
      provider: stackgres
```

```bash
kubectl apply -f cluster.yaml
```

After the operator reconciles, read the name of the generated Secret from the
cluster status:

```bash
kubectl get sgcluster -n my-cluster cluster \
  -o jsonpath='{.status.binding.name}'
```

Inspect the Secret to confirm it contains the expected keys:

```bash
kubectl get secret -n my-cluster \
  "$(kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.status.binding.name}')" \
  -o yaml
```

## How it works

When `spec.configurations.binding` is present, the operator creates or updates a
Secret (in the same namespace as the cluster) that conforms to the
[provisioned-service contract](https://servicebinding.io/spec/core/1.0.0/#provisioned-service):
the Secret contains keys such as `host`, `port`, `database`, `username`,
`password`, and `type`. The operator writes the Secret name into
`status.binding.name`, which is the standard field a Service Binding runtime
reads when resolving a `ServiceBinding` resource that targets this `SGCluster`.

All fields under `spec.configurations.binding` are optional and updatable. The
operator re-reconciles the Secret whenever the section changes; no cluster restart
is required.

## What to expect

- The operator creates the binding Secret within one reconciliation cycle (typically
  a few seconds) after the patch is applied.
- Verify the Secret is present and the status field is populated:

  ```bash
  kubectl get sgcluster -n my-cluster cluster \
    -o jsonpath='{.status.binding.name}'
  ```

- A Service Binding runtime that watches `SGCluster` as a provisioned service will
  automatically pick up the Secret and inject it into bound application Pods.

## Pitfalls

- **The consuming side must be Service Binding-aware.** The operator creates and
  maintains the Secret, but injection into application Pods requires a Service
  Binding-compatible runtime or operator (for example the
  [Service Binding Operator](https://github.com/redhat-developer/service-binding-operator)).
  Without it, applications must consume the Secret manually.
- **The referenced password Secret must exist before applying the patch.** If
  `configurations.binding.password.name` references a Secret that does not exist
  in the namespace, the operator cannot populate the binding Secret and will report
  a reconciliation error. Create the application-user Secret first.
- **Username and database are not created by the operator.** Setting `username` and
  `database` in `spec.configurations.binding` only controls what is written into the
  binding Secret; it does not create the Postgres user or database. Use an
  [SGScript]({{% relref "06-crd-reference/10-sgscript" %}}) or another mechanism to
  provision the user and database before binding.
- **Deleting the section removes the Secret.** If you remove
  `spec.configurations.binding`, the operator drops the generated Secret. Ensure no
  active `ServiceBinding` resources still reference it before removing the section.
