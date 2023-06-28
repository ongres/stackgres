---
title: Pre-Requisites
weight: 1
url: /developer/prerequisites
description: Details about the necessary tools to build and use StackGres.
---

The core of the operator is written purely in Java and uses the Quarkus framework crafted from the best of breed Java libraries and standards.
The Java runtime can be compiled into a native image using GraalVM.

You need the following to start developing on StackGres:

* [Java OpenJDK](https://adoptium.net/) 11 or higher. Alternatively, you could use the JDK from GraalVM.
* [GraalVM Community Edition](https://github.com/graalvm/graalvm-ce-builds/releases/tag/vm-21.0.0.2) 21.0.0 (Optional. Required for native image builds).
* [Maven](https://maven.apache.org/) 3.6.2 or higher (Optional. The mvnw script wrapper can be used).
* [Docker](https://docs.docker.com/install/) 20.10.6 or higher (Recommended. Required for integration and e2e tests).
* [Kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) v1.18 or higher (Optional. Required for e2e tests outside of docker).
* [Helm](https://helm.sh/docs/intro/install/) 3.6.0 or higher (Optional. Required for e2e tests outside of docker or installation of charts).
* [Kind](https://github.com/kubernetes-sigs/kind) 0.11.1 or higher (Optional. Required for e2e tests).

StackGres is a regular Maven project, and you can use any IDE you like.
Our team uses Eclipse and/or IntelliJ IDEA.
