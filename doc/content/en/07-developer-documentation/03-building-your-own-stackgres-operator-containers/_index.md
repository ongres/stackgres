---
title: Building StackGres operator images
weight: 3
url: developer/stackgres/build/operator/own
description: Details about how to build the container images.
showToc: true
---

Building StackGres container images is fairly simple.
You can build them in two variants:

 * JVM: Using OpenJDK JVM as the base image (default)
 * Native: Using Ubi8 Minimal as the base image

## Building the StackGres JVM Image

Using the JVM build compiles faster but leads to a higher memory footprint in the running instance. So, this is useful for development and testing in local environments.

You can build the JVM versions using the script in the corresponding components (such as operator, REST API, etc.):

```
stackgres-k8s/src/operator/src/main/docker/build-image-jvm.sh
```

You can find the image in the `stackgres/operator` repository, tagged as `main-jvm`.

## Building the StackGres Native Image

Building a native image takes longer to compile, but the container will have a lower memory footprint.
This is the recommended approach for production workloads.

The native image uses GraalVM but at the expense of some limitations.
So once your development is ready, be sure to test it with the native image build.

To build a native image, run:

```
stackgres-k8s/src/operator/src/main/docker/build-image-native.sh
```

The native images are tagged as `main`.
