# Helm Chart to create the StackGres Cluster

## Requirements
- The Kubernetes cluster to install the operator version >= `1.12.0`
- [Helm](https://helm.sh/docs/using_helm/#installing-helm) version >= `2.14.3`
- Tiller - cluster-side service
  ```
  helm init --history-max 20
  ```
  > If you´re using helm to deploy all your applications raise the value of `history-max` parameter
- [Install the StackGres-Operator](https://gitlab.com/ongresinc/stackgres/tree/development/operator/install/kubernetes/chart/stackgres-operator)

## Create the StackGres Cluster

Run the next command (make sure you are in the root path of the repository).

`helm install --name stackgres operator/install/kubernetes/chart/stackgres-cluster/`

> Note: If you want to se the execution without installing, add `--dry-run --debug` at the end

## Configurable parameters

| Parameter | Default value |
|-----------|---------------| 
| `postgresql.version` | "11.5" |
| `cluster.name` | "stackgres" |
| `instance` | 1 |
| `pgconfig` | "postgresconf" |
| `profile` | "size-xs" |

> Note: for testing purpose use the default value, for production purpose use a stable release. 


## List the chart deployment 
`helm list` or `helm ls --all`

## Delete the StackGres Cluster

`helm delete --purge stackgres`








