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

- docker

#### Compiling and running

The build process is bootstraped in a maven profile, to run the build:

```
./mvnw clean package -P build-image-jvm
```

The image is loaded in local docker registry. You will have to upload the generated image to the registry used
by kubernetes. Then to deploy the operator run from the project roor folder:

```
helm install stackgres-cluster --namespace stackgres stackgres-k8s/install/helm/stackgres-cluster
```

### Building locally

#### Prerequisites

The prerequisites are the same for any Quarkus-based application.

- JDK 17+ installed with `JAVA_HOME` configured appropriately.
- GraalVM installed from the GraalVM web site. Using the community edition is enough.
- The `GRAALVM_HOME` environment variable configured appropriately.
- The `native-image` tool must be installed; this can be done by running `gu install native-image` from your GraalVM directory.
- A working C developer environment.

#### Compiling and running

To create the native executable you can use

```
./mvnw package -P native,build-image-native
```

The image is loaded in local docker registry. You will have to upload the generated image to the registry used
by kubernetes. Then to deploy the operator run from the project roor folder:

```
helm install stackgres-cluster --namespace stackgres stackgres-k8s/install/helm/stackgres-cluster
```

#### Code Conventions

To validate all the static code analysis rules and code conventions against the project you can use

```
./mvnw clean verify -P safer
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

#### IDE plugins

- [Immutables](https://immutables.github.io/getstarted.html)

## Updating k8s objects in the CRDs

To update CRDs with the swagger of latest k8s version supported by StackGres:

```shell
sh stackgres-k8s/ci/utils/update-crds.sh
```

To manually add the definition of a k8s 1.28 object inside of a CRD:

```shell
K8S_VERSION=1.28 sh stackgres-k8s/ci/utils/utils get_k8s_object_as_yaml io.k8s.api.core.v1.NodeAffinity 26
```

When adding a definition manually to the CRD YAML you will have to update the relative description in order to include a reference to the official reference documentation URL.
 Following the example above would be:

```yaml
                    nodeAffinity:
                      description: |
                        Node affinity is a group of node affinity scheduling rules.
                        
                        See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.28/#nodeaffinity-v1-core
```
