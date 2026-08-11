# e2e tests

End-to-end tests are a means to test StackGres operator in a real kubernetes cluster.

## Run a test

```
sh run-test.sh <spec script file path>
```

## Run all tests

```
sh run-all-tests.sh
```

## Run compatibility matrix

```
sh comp-matrix.sh
```

## Run a util function

For ease of use there are some functions to inspect and watch kubernetes cluster status and e2e
 test's logs. Those functions are defined inside `utils` folder.
To use these functions just run the following command:

```
sh e2e <function name>
```

## Run on CI

CI uses a meven profile to run e2e tests in docker. You can test e2e locally using docker with the following command:

```
 ./mvnw clean verify -P build-image-jvm,integration -Dit.test=StackGresOperatorEnd2EndIt
```

e2e test can also be used with native image:

```
./mvnw clean verify -P native,build-image-native,integration -Dit.test=StackGresOperatorEnd2EndIt
```

## Environment variables

Some environment variables allow to control how e2e test behave:

* `IMAGE_TAG`: The tag of the operator image to use in the e2e test (default: main-jvm).
* `EXTENSIONS_REPOSITORY_URL`: Allow to set a different URL for extensions repository.
* `E2E_ENV`: This set the environment to script to use in order to setup the kubernetes cluster (default: kind).
* `CONTAINER_ENGINE`: The container engine used to build, pull, push and inspect the images (default: docker). It is
 used as a command prefix, so a value carrying arguments like `podman --remote` works everywhere except in the kind
 environment, that executes the engine by name. See [Kind](#kind) for the requirements of `podman`.
* `REGISTRY_AUTH_FILE`: The file holding the registries credentials, the one `podman` reads (default:
 `$HOME/.docker/config.json`, the file `docker` writes).
* `E2E_TIMEOUT`: Some operation wait on pods to be running or terminated. This environment variable controls the timeout in seconds of those operations (default: 3 minutes).
* `E2E_PARALLELISM`: The number of test to run in parallel with `run-all-tests.sh` (default: `$(( $(getconf _NPROCESSORS_ONLN) / 8 ))`).
* `E2E_BUILD_IMAGES`: To avoid rebuilding the operator set this environment variable to false (default: true).
* `E2E_FORCE_IMAGE_PULL`: To force operator and components image pull from registries before tests starts.
* `E2E_OPERATOR_REGISTRY`: The operator images repository to pull images from and rename to required images.
* `E2E_OPERATOR_REGISTRY_PATH`: The operator images path to use to pull images from and rename to required images.
* `E2E_COMPONENTS_REGISTRY`: The components images repository to pull images from and rename to required images.
* `E2E_COMPONENTS_REGISTRY_PATH`: The components images path to use to pull images from and rename to required images.
* `E2E_REUSE_OPERATOR_PODS`: To avoid recreating the operator set this environment variable to true to reuse an installed operator if already exists (default: true).
* `E2E_SKIP_UPGRADE_FROM_PREVIOUS_OPERATOR`: To avoid installing the previous version of the operator and perform an upgrade set this to true (default: false).
* `E2E_OPERATOR_OPTS`: To pass extra parameters to the operator helm chart use this variable.
* `E2E_DISABLE_LOGS`: To diable logs of pods set this variable to true (default: false).
* `E2E_DISABLE_CACHE`: To disable the use of local docker as a cache for images set this variable to true (default: false).
* `E2E_NPM_BUILD_SKIP_USER_MOUNT`: Doesn't mount the local user (and its home directory nor the `/etc/` files) into the npm build container
* `K8S_VERSION`: This set the kubernetes cluster version to setup (default: 1.16.15).
* `K8S_REUSE`: Kubernetes cluster setup can be very expensive in terms of time. Set this environment variable to true to reuse a kubernetes cluster if already exists (default: false).
* `K8S_EXTRA_PORT`: Allow to define a port to expose in the kind docker container with following format: `<node port>:<local port>:<local listening address>:<port protocol>`.
* `K8S_USE_INTERNAL_REPOSITORY`: Allow to bypass local docker repository and pull images directly to internal Kubernetes repository (only if `$E2E_ENV` support it).
* `KIND_CONTAINERD_CACHE_PATH`: Allow to set a local path to use as containerd's repository for kind environment. Doing so will allow to re-use the repository among restart of kind even with different versions.
* `K8S_FROM_DIND`: Set to true to use docker internal IPs for kubernetes configuration to access the kind cluster
 (some systems like macos or windows will not work with this but it is useful to run e2e in docker). This is only
 tested with docker.
* `SKIP_SPEC_INSTALL`: Set this to true to skip call of function `e2e_test_install` (default: false).
* `SKIP_SPEC_UNINSTALL`: Set this to true to skip call of function `e2e_test_uninstall` (default: false).


### Kind

Those environment variable affect the e2e test only if kind environment is used.

* `KIND_NAME`: The name of the kind cluster.

#### Running kind with podman

Set `CONTAINER_ENGINE=podman` and the kind environment will select the podman provider of kind. Since kind executes
 the engine by name the value can not carry any argument, so `podman --remote` is rejected. On top of that:

* `/dev/net/tun` must be present: kind creates its own bridge network and the network backend of podman needs that
 device to set it up. This is what an unprivileged container lacks.
* A rootless podman requires the cgroups v2.
* Kubernetes 1.20 and below use kind v0.15.0, that predates the support for podman, and is therefore rejected.

Two things to know once it runs:

* `kind delete cluster` does not remove the network it created, run `podman network rm kind` to get rid of it.
* Calico may not work on a rootless node since it requires kernel modules and sysctls that may not be available.
 Set `K8S_DISABLE_CALICO=true` if the pods stay pending.

Paths passed through `KIND_CONTAINERD_CACHE_PATH`, `KIND_LOG_HOST_PATH` and `KIND_EXTRA_MOUNTS` are written directly
 by the user running the e2e test when the engine is podman, instead of by a container running as root, so they have
 to be writable by that user.

## Write a test

A test is a sequence of commands that must pass some checks written in a spec script file inside the `spec` folder.
Each test start a stackgres cluster in the namespace with the same name of the spec script plus a prefix of the
 current timestamp in hexadecimal.
To write a test create a spec script file in `spec` folder and implement following functions (they must be posix 
 compatible shell scripts):

* `e2e_test`: The main test function. Inside this function you should call the `run_test` function followed by a method
 name declared in the same spec script file and run your commands there. If any command fail in a test the test must
 fail. The only exception to this rule is when a check on a command is required by the test, in such case return a 1
 in case the test failure is due to this check.

* `e2e_test_before_all` (optional): This function run before all tests. Used to setup test dependent resources.

* `e2e_test_after_all` (optional): This function run after all tests. Used to tear down test dependent resources.

* `e2e_test_install_pods` (optional): This function print the number of pod to expect running before starting the test
 (is used by `test_install`).

* `e2e_test_install` (optional): This function allow to overwrite the cluster creation.

* `e2e_test_uninstall` (optional): This function allow to overwrite the cluster cleanup.

A YAML with installation values to use to install the cluster can be created using the name `<spec script file name>.values.yaml`.

## Environments

The default kubernetes cluster is kind but there are some more available:

* [kind](https://kind.sigs.k8s.io/)
* [k3d](https://github.com/rancher/k3d)
* [minikube](https://github.com/kubernetes/minikube)
* [minishift](https://github.com/kubernetes/minikube)
* [gke](https://cloud.google.com/kubernetes-engine)
* [eks](https://aws.amazon.com/eks/)
* [aks](https://docs.microsoft.com/en-us/azure/aks/)
* current (use currently configured k8s cluster)

Docker is required in order to use the k3d environment. The kind environment works with docker and with podman,
 see [Running kind with podman](#running-kind-with-podman).

### Support for other k8s clusters

Kubernetes cluster support can be achieved by creating a new environment script in
 `envs` folder that setup the environment variables needed to access the kubernetes 
 cluster using `kubectl` and implement the following function (they must be posix 
 compatible shell scripts):

* `reset_k8s`: Create the kubernetes cluster if not exists and setup system in order to access
 the kubernetes cluster using `kubectl`.
* `reuse_k8s`: Setup the system in order to access the kubernetes cluster using
 `kubectl`.
* `delete_k8s`: Delete the kubernetes cluster.
* `load_image_k8s`: Load an image from a local docker registry (currently the
 project build the image and store it there) to the kubernetes cluster.
