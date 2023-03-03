---
title: Building StackGres
weight: 2
url: developer/stackgres/build
description: Details about how to build the operator.
showToc: true
---

StackGres is hosted on [GitLab](https://gitlab.com/ongresinc/stackgres), but there's also a [GitHub mirror repository](https://github.com/ongres/stackgres) available.
For cloning and building, you can use either.
For submitting merge requests, you need to use the GitLab repository.

## Get the Source Code

Clone the sources from GitLab (or alternatively, GitHub):

```bash
git clone https://gitlab.com/ongresinc/stackgres.git
cd stackgres/
```

Build StackGres via Maven (using the `mvnw` wrapper):

```bash
cd stackgres-k8s/src/
./mvnw clean install
```

## Build With Checks

In order to contribute to StackGres, you need to build with code checks.
The StackGres CI will also run these checks.
Run the Maven build with the `safer` profile:

```bash
./mvnw clean install -P safer
```
