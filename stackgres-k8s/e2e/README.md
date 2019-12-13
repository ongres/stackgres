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
mvn clean verify -P build-image-jvm,integration -Dit.test=StackGresOperatorEnd2EndIt
```

e2e test can also be used with native image:

```
mvn clean verify -P native,build-image-native,integration -Dit.test=StackGresOperatorEnd2EndIt
```

## Environment variables

Some environment variables allow to control how e2e test behave:

* `ENV`: This set the environment to script to use in order to setup the kubernetes cluster.
* `KUBERNETES_VERSION`: This set the kubernetes cluster version to setup (default: 1.12).
* `IMAGE_TAG`: The tag of the operator image to use in the e2e test (default: development-jvm).
* `TIMEOUT`: Some operation wait on pods to be running or terminated. This environment variable controls the timeout in seconds of those operations (default: 3 minutes).
* `DEBUG_OPERATOR`: Enable operator debug (you must rebuild the operator image for this to work).
* `DEBUG_OPERATOR_SUSPEND`: Suspend operator JVM Enable operator debug (you must rebuild the operator image for this to work).
* `REUSE_K8S`: Kubernetes cluster setup can be very expensive in terms of time. Set this environment variable to true to reuse a kubernetes cluster if already exists.
* `REUSE_OPERATOR`: To avoid recreating the operator set this environment variable to true to reuse an installed operator if already exists.
* `BUILD_OPERATOR`: To avoid rebuilding the operator set this environment variable to false.
* `RESET_NAMESPACES`: Set this to false to disable namespaces reset.

### Kind

Those environment variable affect the e2e test only if kind environment is used.

* `KIND_NAME`: The name of the kind cluster.
* `KIND_NAME`: The name of the kind cluster.
* `USE_KIND_INTERNAL`: Set to true to use docker internal IPs for kubernetes configuration to access the kind cluster (some systems like mac and windows will not work with this).

## Write a test

A test is a sequence of commands that must pass some checks written in a spec script file inside the `spec` folder.
Each test start a stackgres cluster in the namespace with the same name of the spec script.
To write a test create a spec script file in `spec` folder and implement following functions (they must be posix 
 compatible shell scripts):

* `test`: The main test function. Inside this function you should call the `run_test` function followed by a method
 name declared in the same spec script file and run your commands there. If any command fail in a test the test must
 fail. The only exception to this rule is when a check on a command is required by the test, in such case return a 1
 in case the test failure is due to this check.

* `test_before_all` (optional): This function run before all tests. Used to setup test dependent resources.

* `test_after_all` (optional): This function run after all tests. Used to tear down test dependent resources.

* `test_install_pods` (optional): This function print the number of pod to expect running before starting the test
 (is used by `test_install`).

* `test_install` (optional): This function allow to overwrite the cluster creation.

A YAML with installation values to use to install the cluster can be created using the name `<spec script file name>.values.yaml`.

## Environments

### Kind

Currently the only supported kubernetes cluster is [kind](https://kind.sigs.k8s.io/).

Docker is required in order to use the kind environment.

### Support for other k8s clusters

Kubernetes cluster support can be achieved by creating a new environment script in
 `envs` folder that setup the environment variables needed to access the kubernetes 
 cluster using `kubectl` and implement the following function (they must be posix 
 compatible shell scripts):

* `reset_k8s`: Create the kubernetes cluster and setup system in order to access
 the kubernetes cluster using `kubectl`.
* `reuse_k8s`: Setup the system in order to access the kubernetes cluster using
 `kubectl`.
* `load_operator_k8s`: Load an image from a local docker registry (currently the
 project build the image and store it there) to the kubernetes cluster.
