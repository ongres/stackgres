# StackGres Operator

The StackGres Operator is built in pure-Java and uses the [Quarkus](https://quarkus.io/) framework,
a Kubernetes Native Java stack tailored for GraalVM & OpenJDK HotSpot, crafted from the best of
breed Java libraries and standards.

## Building

The project is built by the `stackgres-k8s/ci/build/build.sh` script. It builds a hierarchy of
 modules, each one inside a container, and caches the result in a registry using a hash of the
 module sources, so that a module is rebuilt only when it or one of its dependencies changes.
 Modules are defined under the `.modules` section of `stackgres-k8s/ci/build/config.yml`. See
 [the build system documentation](../ci/build/README.md) for the details.

### Prerequisites

- docker

Every tool needed to drive the build (the docker CLI, helm, kubectl, kind, git, jq, yq, ...) is
 already installed in the CI image, and the `stackgres-k8s/ci/build/ciw` wrapper runs any command
 inside that image, mounting the project folder and the docker socket. Each module is then compiled
 in its own build image, so neither a JDK, nor Maven, nor Node have to be installed locally.

### Building a module

All the commands below are run from the project root folder:

```
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh <module> [<module> ...]
```

When no module is specified all of them are built. Some examples:

```
# Build the operator JVM image and everything it depends on
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh operator-jvm-image

# Build the operator native image
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh operator-native-image

# Run the unit tests of the common and the operator Java modules
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh common-java-test operator-java-test

# Build the web console and the helm packages
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh admin-ui helm-packages

# Print the hash of each module without building anything
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/build/build.sh hashes
```

The images are loaded in the local docker registry. You will have to upload the generated images to
 the registry used by kubernetes. Then, to deploy the operator, run from the project root folder:

```
helm install --create-namespace --namespace stackgres stackgres-operator \
  stackgres-k8s/install/helm/stackgres-operator
```

### Building locally with Maven

For a faster edit-compile-test cycle the Java modules can also be built directly with Maven from
 this folder. This requires JDK 21+ installed with `JAVA_HOME` configured appropriately and, to
 build a native executable, GraalVM with the `native-image` tool installed and a working C
 developer environment.

```
./mvnw -pl <module> -am install
```

Always pass `-am`, otherwise the module is compiled against a stale installed `common` artifact.

To create the native executable of a module:

```
./mvnw -pl operator -am package -P native
```

The container image is not built by Maven: use `build.sh` with the corresponding `*-jvm-image` or
 `*-native-image` module.

### Code conventions

To validate all the static code analysis rules and code conventions against the project you can use

```
./mvnw clean verify -P safer
```

### IDE plugins

- [Immutables](https://immutables.github.io/getstarted.html)

## Running the e2e tests

The end-to-end tests live under `stackgres-k8s/e2e` and are driven by the `stackgres-k8s/e2e/e2e`
 CLI. They can be run through the same `ciw` wrapper, that already includes `kind`, `kubectl` and
 `helm`. Set `K8S_FROM_DIND=true` so that the kubernetes configuration uses the docker internal IPs
 to reach the kind cluster that is created from inside the container:

```
K8S_FROM_DIND=true sh stackgres-k8s/ci/build/ciw \
  sh stackgres-k8s/e2e/e2e cli pull test -- stackgres-k8s/e2e/spec/dbops-pgbench
```

`pull` and `test` are the phases to execute (`pull` retrieves the operator and the component images
 from the registries and `test` runs the specs), and everything after `--` is the list of specs to
 run. When no spec is specified all the specs under `stackgres-k8s/e2e/spec` are executed.

Run `sh stackgres-k8s/e2e/e2e cli --help` for the complete list of phases and options and see
 [the e2e documentation](../e2e/README.md) for the environment variables that control the tests.

## CI utilities

The `stackgres-k8s/ci/utils` folder holds the scripts used to maintain the project:

| Script | Description |
|--------|-------------|
| `update-version.sh <version> [<main tag version>]` | Set the version of the Java modules, the helm charts, the image tags and the documentation |
| `update-pom-versions.sh` | Update the Maven properties and plugins to the latest available versions |
| `update-base-images.sh` | Update the base images used by the build to the latest available versions |
| `pin-base-images.sh` | Pin the base images used by the build to their current digest |
| `update-crds.sh` | Update the k8s object definitions in the CRDs using the swagger of the latest k8s version supported |
| `crds2description_json.sh <crds path> <output path>` | Extract the descriptions of the CRDs to JSON files |
| `update-csi-driver-host-path.sh` | Regenerate the vendored csi-driver-host-path manifests used by the kind e2e environment |
| `generate-release-template.sh <version>` | Generate the release notes template of a version |
| `glabw` | Wrapper to run the `glab` GitLab CLI in a container |
| `utils` | Library of functions used by the other scripts |

They also require the tools of the CI image, so run them through `ciw` too:

```
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/utils/update-crds.sh
```

### Updating k8s objects in the CRDs

To update the CRDs with the swagger of the latest k8s version supported by StackGres:

```shell
sh stackgres-k8s/ci/build/ciw sh stackgres-k8s/ci/utils/update-crds.sh
```

To manually get the definition of a k8s object to add it inside a CRD (the k8s version defaults to
 the latest one supported by the operator helm chart and the last parameter is the indentation):

```shell
K8S_VERSION=1.36 sh stackgres-k8s/ci/build/ciw \
  sh stackgres-k8s/ci/utils/utils get_k8s_object_as_yaml io.k8s.api.core.v1.NodeAffinity 26
```

When adding a definition manually to the CRD YAML you will have to update the relative description
 in order to include a reference to the official reference documentation URL. Following the example
 above would be:

```yaml
                    nodeAffinity:
                      description: |
                        Node affinity is a group of node affinity scheduling rules.
                        
                        See: https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.36/#nodeaffinity-v1-core
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
