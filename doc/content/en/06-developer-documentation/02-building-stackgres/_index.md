---
title: Building StackGres
weight: 2
---

# Building StackGres
Build stackgres images are fairly simple. You can build in two forms:
 * build a using zulu JVM as a base image (Default)
 * compa

## Building stackgres zulu based image

Compiles fast but has a higher memory footprint. So it useful for local environment testing

Go to the stackgres/stackgres-k8s/src folder and the execute:

```
mvn clean verify -P build-image-jvm
```

You can find the image in th stackgres/operator repository tagged as development-jvm

## Building stackgres native image

Takes long to compile but has a lower memory footprint.  Is recommended for production workloads. 

The native image use GraalVM but at the expense of some limitations. So once your development is ready, be sure to test it with the native image. 

To generate a native image simpleme run:

```
mvn clean verify -P native,build-image-native
```


