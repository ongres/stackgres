---
title: Running the e2e tests
weight: 5
url: developer/stackgres/e2e
description: Details about how to run E2E tests.
showToc: true
---

Given the operator nature we rely heavily on our integration and e2e tests. 

E2E tests are mainly composed of POSIX complaint scripts (only exception is the use of local variables in functions),
 and we intend to keep them that way. 

The easiest way to run the e2e scripts is by executing the `stackgres-k8s/e2e/run-all-tests.sh` file.
 This script will configure a kuberentes cluster (by default kind), then it will generate jvm version
  of the operator and deploy it on the configured kubernetes cluster. 

```
sh stackgres-k8s/e2e/run-all-tests.sh
```

There is also several util functions which are localted in the folder stackgres-k8s/e2e/utils

## E2E tests with kind

By default e2e tests are made with kind, so you don't need to specify anything to use it other than having kind installed.

## E2E tests with other environment

Environment can be specified using the `ENV` environment variable. Currently we support following environment:

* kind
* minikube
* gke

