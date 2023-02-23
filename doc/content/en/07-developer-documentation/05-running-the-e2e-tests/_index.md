---
title: Running the e2e tests
weight: 5
url: developer/stackgres/e2e
description: Details about how to run E2E tests.
showToc: true
---

Given the operator nature, we rely heavily on integration and end-to-end (e2e) tests.

E2E tests are built with shell scripts.
They are mainly POSIX-compliant scripts (only exception is the use of local variables in functions).

The easiest way to run the e2e scripts is by executing the `stackgres-k8s/e2e/run-all-tests.sh` file:

```bash
sh stackgres-k8s/e2e/run-all-tests.sh
```

This will configure a Kubernetes cluster (by default using kind), build the JVM version of the operator, and deploy it to the configured Kubernetes cluster.

The E2E tests are grouped by specifications, which are located in the `stackgres-k8s/e2e/spec` folder.

There are also several util functions, which are located in the `stackgres-k8s/e2e/utils` folder.

For more information, have look at the E2E [readme](https://gitlab.com/ongresinc/stackgres/-/tree/main/stackgres-k8s/e2e).

## E2E Tests With Kind

By default, e2e tests use kind to create and configure a Kubernetes cluster, so you don't need to specify anything other than having kind installed.

## E2E Tests With Other Environments

The Kubernetes environment can be specified using the `E2E_ENV` environment variable. Currently we support the following environments:

* `kind` (default)
* `minikube`
* `gke`

