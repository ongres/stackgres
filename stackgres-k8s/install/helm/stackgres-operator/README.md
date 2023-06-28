# Helm Chart to create the StackGres Operator

## Requirements

* The Kubernetes cluster to install the operator
* [Helm](https://helm.sh/docs/using_helm/#installing-helm) version >= `3.1.1`

## Create the StackGres Operator

Run the next command (make sure you are in the root path of the [repository](https://gitlab.com/ongresinc/stackgres)).

`helm install --namespace stackgres stackgres-operator operator/install/kubernetes/chart/stackgres-operator/`

## Configurable parameters

See https://stackgres.io/doc/latest/install/helm/

## List the chart deployment

`helm list` or `helm ls --all`

## Delete the StackGres Operator

See https://stackgres.io/doc/latest/administration/uninstall/

# Developers

Documentation is generate by [helm-docs](https://github.com/norwoodj/helm-docs):

```
helm-docs -o VALUES.md -f values.yaml -t VALUES.md.gotmpl
```