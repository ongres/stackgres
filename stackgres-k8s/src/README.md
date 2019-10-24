# StackGres Operator

The StackGres Operator is build in pure-Java and uses the [Quarkus](https://quarkus.io/) framework a Kubernetes
Native Java stack tailored for GraalVM & OpenJDK HotSpot, crafted from the best of breed Java
libraries and standards.

## Building

To build the operator you need to have GraalVM (or any Java SDK 8+ if you do not need to build the native operator)
installed locally or directly use a container to bootstrap the compile phase.
The native-image generation has been tested on Linux only but should also work on macOS.

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

```
   _____ _             _     _____
  / ____| |           | |   / ____|
 | (___ | |_ __ _  ___| | _| |  __ _ __ ___  ___
  \___ \| __/ _` |/ __| |/ / | |_ | '__/ _ \/ __|
  ____) | || (_| | (__|   <| |__| | | |  __/\__ \
 |_____/ \__\__,_|\___|_|\_\\_____|_|  \___||___/
                                  by OnGres, Inc.

```