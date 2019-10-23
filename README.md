# StackGres

[![master status](https://gitlab.com/ongresinc/stackgres/badges/master/pipeline.svg?style=flat-square)](https://gitlab.com/ongresinc/stackgres/commits/master)

> StackGres - Enterprise-grade, Full Stack PostgreSQL on Kubernetes

StackGres is a full stack PostgreSQL distribution for Kubernetes, packed into an easy deployment unit.
With a carefully selected and tuned set of surrounding PostgreSQL components.

An enterprise-grade PostgreSQL stack needs several other ecosystem components and significant tuning.
It's not only PostgreSQL. It requires connection pooling, automatic failover and HA, monitoring,
backups and DR, centralized logging… we have built them all: a Postgres Stack.

Postgres is not just the database. It is also all the ecosystem around it. If Postgres would be the
Linux kernel, we need a PostgreSQL Distribution, surrounding PostgreSQL, to complement it with the
components that are required for a production deployment. This is what we call a PostgreSQL Stack.
And the stack needs to be curated. There are often several software for the same functionality. And
not all is of the same quality or maturity. There are many pros and cons, and they are often not
easy to evaluate. It is better to have an opinionated selection of components, that can be packaged
and configured to work together in a predictable and trusted way.

## Operator

This repository holds one of the major components around StackGres, and is the StackGres Operator
build around Kubernetes. An Operator is a method of packaging, deploying and managing a Kubernetes
application. Some applications, such as databases, required more hand-holding, and a cloud-native
Postgres requires an operator to provide additional knowledge of how to maintain state and integrate
all the components.

This operator is build in pure-Java and uses the [Quarkus](https://quarkus.io/) framework a Kubernetes
Native Java stack tailored for GraalVM & OpenJDK HotSpot, crafted from the best of breed Java
libraries and standards.

The container image of StackGres is built on Red Hat Universal Base Image, and compiled as a native binary
with GraalVM allowing amazingly fast boot time and incredibly low RSS memory.

### Getting Started

You can deploy the StackGres operator by using [helm](https://helm.sh/):

```
helm template --name stackgres-operator operator/install/kubernetes/chart/stackgres-operator | kubectl create -f -
```

Once the operator is up and running in your Kubernetes cluster you can deploy new clusters using

```
helm template --name my-cluster operator/install/kubernetes/chart/stackgres-cluster | kubectl create -f -
```

you can edit the instances field with the number of replicas you want to deploy.

## Building

For build this project you need to have GraalVM installed locally or directly use a container to bootstrap
the compile phase. The native-image generation has been tested on Linux only but should also work on macOS.

### Building using a container

#### Prerequisites

- buildah
- podman

#### Compiling and running

The build process is bootstraped in a multistage Dockerfile, to run the build:

```
buildah bud -f src/main/docker/Dockerfile.multistage -t stackgres/operator .
```

This multistage Dockerfile also generates a container image with the operator.
To run the generated container image just use:

```
podman run -i --rm -p 8080:8080 localhost/stackgres/operator
```

Once you start the operator, it start watching the CRD resource for StackGres that can be deployed using:

```
helm template --name stackgres-cluster operator/install/kubernetes/chart/stackgres-cluster | kubectl create -f -
```

### Building locally

#### Prerequisites

The prerequisites are the same for any Quarkus-based application.

- JDK 1.8+ installed with `JAVA_HOME` configured appropriately.
- GraalVM installed from the GraalVM web site. Using the community edition is enough.
- The `GRAALVM_HOME` environment variable configured appropriately.
- The `native-image` tool must be installed; this can be done by running `gu install native-image` from your GraalVM directory.
- A working C developer environment.

#### Compiling and running

To create the native executable you can use

```
./mvnw package -P native
```

this will generate an artifact called `stackgres-operator-${project.version}-runner`.

You can omit the profile `-P native` to generate a normal jar that can be run using the JVM.

Once you start the operator, it start watching the CRD resource for StackGres that can be deployed using:

```
helm template --name stackgres-cluster operator/install/kubernetes/chart/stackgres-cluster | kubectl create -f -
```

#### Integration tests

Integration tests requires docker to be installed (if not on Linux set the environment variable `DOCKER_HOST` pointing to the protocol, host and port of the docker daemon). To run the ITs:

```
./mvnw verify -P integration
```

---

## FAQ

### Is StackGres a modified version of Postgres?
No. StackGres contains PostgreSQL, plus several other components (such as connection pool or
automatic high availability software) from the PostgreSQL ecosystem. All of them are vanilla
versions, as found in their respective open source repositories, including PostgreSQL.
Any application that runs against a PostgreSQL database should work as-is.

### How is StackGres software licensed?
StackGres source code is licensed under the OSI-approved open source license
GNU Affero General Public License version 3 (AGPLv3). All the source code is available on this
repository.

### Is there a StackGres commercial license that is “GPL-free”?
Yes. Contact us if you want a trial or commercial license that does not contain the GPL clauses.
Will you ever switch from an open-source license to a source-available one?
Our promise is that no, this won’t happen. We respect others who switch to or are directly built
as source-available software, but we don’t follow this approach.
We love the concept of GitLab’s stewardship, and in the same spirit, we promise here that
StackGres will always be open source software.

### What PostgreSQL versions are supported?
As of now, PostgreSQL major version 11 and 12.

### Where does it run?
StackGres has been designed to run on any Kubernetes-certified platform. Whether is a
Kubernetes-as-a-Service offered by a cloud provider or a distribution running on-premise,
StackGres should run as-is.

### How is HA implemented?
High Availability and automatic failover are based on Patroni, a well-reputed and trusted software
for PostgreSQL. No external DCS (Distributed Consistent Storage) is required, as it relies on
K8s APIs for this (which in turns reach etcd).

### Why is used UBI as the base image for StackGres?
Red Hat Universal Base Images (UBI) are OCI-compliant container base operating system images with
complementary runtime languages and packages that are freely redistributable. UBI lets developers
create the image once and deploy anywhere using enterprise-grade packages. For more information read
the official [UBI-FAQ](https://developers.redhat.com/articles/ubi-faq/).

---

```
   _____ _             _     _____
  / ____| |           | |   / ____|
 | (___ | |_ __ _  ___| | _| |  __ _ __ ___  ___
  \___ \| __/ _` |/ __| |/ / | |_ | '__/ _ \/ __|
  ____) | || (_| | (__|   <| |__| | | |  __/\__ \
 |_____/ \__\__,_|\___|_|\_\\_____|_|  \___||___/
                                  by OnGres, Inc.

```