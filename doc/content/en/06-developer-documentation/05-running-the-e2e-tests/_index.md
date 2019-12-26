---
title: Running the e2e tests
weight: 5
---

# Running the E2E tests
Given the operator nature we rely heavily on our integration and e2e tests. 

E2E tests are mainly composed of POSIX complaint scripts, and we intent it to keep that way. 

The easiest way to run the e2e scripts is by executing the run-all-tests.sh file. This script will 
configure a kuberentes cluster (by default kind), then it will generate jvm version of the operator
and deploy it on the configured kubernetes cluster. 

The E2E tests are grouped by specs, and they are contained in the folder stackgres-k8s/e2e/spec. 

There is also several util functions which are localted in the folder stackgres-k8s/e2e/utils

# E2E tests with kind

By default e2e tests are made wih kind, so you don't need to specify anything to use it other than having kind installed.

# E2E tests with minikube 

