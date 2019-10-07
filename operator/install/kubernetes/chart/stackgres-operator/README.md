# Helm Chart to create the StackGres Operator

## Requirements
- The Kubernetes cluster to install the operator
- [Helm](https://helm.sh/docs/using_helm/#installing-helm) version >= `2.14.3`
- Tiller - cluster-side service
  ```
  helm init --history-max 20
  ```
  > If you´re using helm to deploy all your applications raise the value of `history-max` parameter

## Create the StackGres Operator 

Run the next command (make sure you are in the root path of the [repository](https://gitlab.com/stackgres/stackgres)).

`helm install --name stackgres-operator install/kubernetes/chart/stackgres-operator`

## Create the StackGres Operator with a specific version

`helm install --name stackgres-operator --set operatorVersion="0.6"  install/kubernetes/chart/stackgres-operator`

> Note: If you want to se the execution without installing, add `--dry-run --debug` at the end

## Configurable parameters

| Parameter | Default value |
|-----------|---------------|
| `operatorVersion` | master |

> Note: for testing purpose use the default value, for production purpose use a stable release. 


## List the chart deployment 
`helm list` or `helm ls --all`

## Delete the StackGres Operator

`helm delete --purge stackgres-operator`








