---
title: Building your own StackGres operator containers
weight: 3
url: developer/stackgres/build/operator/own
description: Details about how to build the container images.
---

Build stackgres images are fairly simple. You can build in two forms:

 * build using zulu JVM as a base image (Default)
 * build using ubi8 as a base image

## Building stackgres zulu based image

Compiles fast but has a higher memory footprint. So it useful for local environment testing

Go to the `stackgres/stackgres-k8s/src` folder and the execute:

```
./mvnw clean verify -P build-image-jvm
```

You can find the image in th stackgres/operator repository tagged as development-jvm

## Building stackgres native image

Takes long to compile but has a lower memory footprint.  Is recommended for production workloads. 

The native image use GraalVM but at the expense of some limitations. So once your development is ready, be sure to test it with the native image. 

To generate a native image simpleme run:

```
./mvnw clean verify -P native,build-image-native
```
