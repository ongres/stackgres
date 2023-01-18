# StackGres Helm Operator Bundle

This module build the bundle and the images to be used with OLM based on the StackGres Operator Helm Chart.

See also https://sdk.operatorframework.io/docs/building-operators/helm/

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

To create the helm operator image

```
make docker-build
```

To create the helm operator bundle image

```
make bundle-build
```
