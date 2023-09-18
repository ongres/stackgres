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
