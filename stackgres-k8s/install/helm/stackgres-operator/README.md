# Helm Chart to create the StackGres Operator

## Requirements

* The Kubernetes cluster to install the operator
* [Helm](https://helm.sh/docs/using_helm/#installing-helm) version >= `3.1.1`

## Create the StackGres Operator

Run the next command (make sure you are in the root path of the [repository](https://gitlab.com/ongresinc/stackgres)).

`helm install --namespace stackgres stackgres-operator operator/install/kubernetes/chart/stackgres-operator/`

## Configurable parameters

See https://stackgres.io/doc/latest/03-production-installation/02-installation-via-helm/

## List the chart deployment

`helm list` or `helm ls --all`

## Delete the StackGres Operator

`helm uninstall stackgres-operator --namespace stackgres`
