# StackGres Helm Operator Bundle

This module build the bundle and the images to be used with OLM.

See also https://sdk.operatorframework.io/docs/overview/

# Build

To create the bundle in the bundle folder:

```
make bundle
```

# Build for OpenShift

To create the bundle for openshift in the bundle folder:

```
make bundle-openshift
```

# Build images

To create the operator bundle image

```
make bundle-build
```

# Deploy images

## OperatorHub

To deploy to [OperatorHub operators reporitory](https://github.com/k8s-operatorhub/community-operators):

```
FORK_GIT_URL="<URL of fork for https://github.com/k8s-operatorhub/community-operators>" sh deploy-to-operatorhub.sh
```

### Test OperatorHub pipeline locally

See `Vagrantfile.operatorhub-test-suite` file notes.

## Red Hat Marketplace

To deploy to [Red Had Marketplace operators reporitory](https://github.com/redhat-openshift-ecosystem/redhat-marketplace-operators):

```
FORK_GIT_URL="<URL of fork for https://github.com/redhat-openshift-ecosystem/redhat-marketplace-operators>" sh deploy-to-red-hat-marketplace.sh
```

### Test RedHat Marketplace pipeline locally

Use `../openshift-certification/start-openshift-operator-certification-pipeline.sh` script (required [`crc`](https://github.com/crc-org/crc) to be installed).

## Red Hat Certified

To deploy to [Red Had Certified operators reporitory](https://github.com/redhat-openshift-ecosystem/certified-operators):

```
FORK_GIT_URL="<URL of fork for https://github.com/redhat-openshift-ecosystem/certified-operators>" sh deploy-to-red-hat-certified.sh
```

### Test RedHat Certified pipeline locally

Use `../openshift-certification/start-openshift-operator-certification-pipeline.sh` script (required [`crc`](https://github.com/crc-org/crc) to be installed).
