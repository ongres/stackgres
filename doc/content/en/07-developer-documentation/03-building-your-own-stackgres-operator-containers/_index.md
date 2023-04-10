---
title: Building your own StackGres operator containers
weight: 3
url: developer/stackgres/build/operator/own
description: Details about how to build the container images.
showToc: true
---

Build stackgres images are fairly simple. You can build in two forms:

 * JVM: Using OpenJDK JVM as the base image (default)
 * Native: Using Ubi8 Minimal as the base image

## Building the StackGres JVM Image

Using the JVM build compiles faster but leads to a higher memory footprint in the running instance. So, this is useful for development and testing in local environments.

You can build the JVM versions using the script in the corresponding components (such as operator, REST API, etc.):

```
stackgres-k8s/src/operator/src/main/docker/build-image-jvm.sh
```

You can find the image in the `stackgres/operator` repository, tagged as `main-jvm`.

## Building stackgres native image

Takes long to compile but has a lower memory footprint.  Is recommended for production workloads. 

The native image use GraalVM but at the expense of some limitations. So once your development is ready, be sure to test it with the native image. 

To generate a native image simpleme run:

```
stackgres-k8s/src/operator/src/main/docker/build-image-native.sh
```

The native images are tagged as `main`.
