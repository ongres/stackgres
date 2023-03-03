---
title: Building StackGres operator images
weight: 3
url: developer/stackgres/build/operator/own
description: Details about how to build the container images.
showToc: true
---

Building StackGres container images is fairly simple.
You can build them in two variants:

 * Using Zulu JVM as the base image (default)
 * Using ubi8 as the base image

## Building the Zulu-Based Image

Using the JVM build compiles faster but has a higher memory footprint. So, this is useful for testing in local environments.

In the `stackgres/stackgres-k8s/src` folder, execute the following:

```bash
./mvnw clean verify -P build-image-jvm
```

You can find the image in the stackgres/operator repository, tagged as development-jvm.

## Building the StackGres Native Image

Building a native image takes longer to compile, but the container will have a lower memory footprint.
This is the recommended approach for production workloads.

The native image uses GraalVM but at the expense of some limitations.
So once your development is ready, be sure to test it with the native image build.

To build a native image, run:

```bash
./mvnw clean verify -P native,build-image-native
```
