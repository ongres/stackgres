---
title: Building StackGres
weight: 2
url: developer/stackgres/build
description: Details about how to build the operator.
showToc: true
---

To build stackgres run the following command inside folder `stackgres-k8s/src`:

## Get the Source Code

Clone the sources from GitLab (or alternatively, GitHub):

```
git clone https://gitlab.com/ongresinc/stackgres.git
cd stackgres/
```

Build StackGres via Maven (using the `mvnw` wrapper):

```
cd stackgres-k8s/src/
./mvnw clean install
```

## Build with checks

Build with strength checks is needed in order to contribute to the project (since the CI will run those checks).
 To do so simply add the `safer` profile:

```
./mvnw clean install -P safer
```
