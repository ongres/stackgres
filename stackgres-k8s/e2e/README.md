# e2e tests

End-to-end tests are a means to test StackGres operator in a real kubernetes cluster.

## Run a test

```
sh run-test.sh <spec script file>
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
