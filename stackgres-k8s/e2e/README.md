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
* `E2E_TIMEOUT`: Some operation wait on pods to be running or terminated. This environment variable controls the timeout in seconds of those operations (default: 3 minutes).
* `E2E_PARALLELISM`: The number of test to run in parallel with `run-all-tests.sh` (default: `getconf _NPROCESSORS_ONLN`).
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
 (some systems like macos or windows will not work with this but it is useful to run e2e in docker).
* `SKIP_SPEC_INSTALL`: Set this to true to skip call of function `e2e_test_install` (default: false).
* `SKIP_SPEC_UNINSTALL`: Set this to true to skip call of function `e2e_test_uninstall` (default: false).


### Kind

Those environment variable affect the e2e test only if kind environment is used.

* `KIND_NAME`: The name of the kind cluster.

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

Docker is required in order to use the kind and k3d environments.

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
