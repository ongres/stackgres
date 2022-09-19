---
title: Uninstall
date:  2021-01-05T10:39:44-03:00
weight: 999999 ## adding a super high value to ensure that this will be the last item
url: administration/uninstall
description: Details about how to uninstall the operator and all its components.
showToc: true
---

## Uninstalling StackGres clusters

### Database clusters

Assuming that your cluster is running on the `default` namespace, execute the commands below to find and delete the clusters:

```bash
## List the available clusters
❯ kubectl get sgcluster -n default
NAME            AGE
my-db-cluster   4m27s

## List the pods for the cluster
❯ kubectl get pods
NAME              READY   STATUS    RESTARTS   AGE
my-db-cluster-0   5/5     Running   1          2m29s
my-db-cluster-1   5/5     Running   1          99s
my-db-cluster-2   5/5     Running   0          74s


## Delete the cluster
❯ kubectl delete sgcluster my-db-cluster -n default
sgcluster.stackgres.io "my-db-cluster" deleted

## Check if the pods were deleted
❯ kubectl get pods -n default
No resources found in default namespace.
```

### Other objects

The `SGCluster` depends on other objects to work properly, such as [instance profiles]({{% relref "/06-crd-reference/02-sginstanceprofile" %}}),
 [postgres configurations]({{% relref "/06-crd-reference/03-sgpostgresconfig" %}}), [connection pooling]({{% relref "/06-crd-reference/04-sgpoolingconfig/" %}}),
 [object storage]({{% relref "/06-crd-reference/10-sgobjectstorage/" %}}), [backups]({{% relref "/06-crd-reference/06-sgbackup/" %}}),
 [distributed logs]({{% relref "/06-crd-reference/07-sgdistributedlogs" %}}), .
 Execute the commands below to find and delete those objects:

```bash
## List all sg* objects:
❯ kubectl get sgobjectstorages,sgbackups,sgdistributedlogs,sginstanceprofiles,sgpgconfigs,sgpoolconfigs -n default
NAME                                                      AGE
sgobjectstorage.stackgres.io/backup-config-minio-backend   162m

NAME                          AGE
sgbackup.stackgres.io/teste   14m

NAME                                                     AGE
sginstanceprofile.stackgres.io/instance-profile-medium   148m
sginstanceprofile.stackgres.io/instance-profile-nano     162m

NAME                                                                             AGE
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609855369232   162m
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609856085474   150m
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609856466466   143m
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609856836573   137m
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609857658946   124m
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609864032670   17m
sgpostgresconfig.stackgres.io/postgres-11-generated-from-default-1609864616518   8m6s
sgpostgresconfig.stackgres.io/postgres-12-generated-from-default-1609864589301   8m33s

NAME                                                                AGE
sgpoolingconfig.stackgres.io/generated-from-default-1609855369294   162m
sgpoolingconfig.stackgres.io/generated-from-default-1609856085523   150m
sgpoolingconfig.stackgres.io/generated-from-default-1609856466511   143m
sgpoolingconfig.stackgres.io/generated-from-default-1609856836622   137m
sgpoolingconfig.stackgres.io/generated-from-default-1609857659076   124m
sgpoolingconfig.stackgres.io/generated-from-default-1609864032716   17m
sgpoolingconfig.stackgres.io/generated-from-default-1609864589347   8m33s
sgpoolingconfig.stackgres.io/generated-from-default-1609864616550   8m6s

## To delete them all:
## IMPORTANT: this WILL remove the backups too!
## PROCEED WITH CARE.
❯ kubectl get sgobjectstorages,sgbackups,sgclusters,sgdistributedlogs,sginstanceprofiles,sgpgconfigs,sgpoolconfigs -n default -o name | xargs kubectl delete
sgobjectstorage.stackgres.io "backup-config-minio-backend" deleted
sgbackup.stackgres.io "teste" deleted
sginstanceprofile.stackgres.io "instance-profile-medium" deleted
sginstanceprofile.stackgres.io "instance-profile-nano" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609855369232" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609856085474" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609856466466" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609856836573" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609857658946" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609864032670" deleted
sgpostgresconfig.stackgres.io "postgres-11-generated-from-default-1609864616518" deleted
sgpostgresconfig.stackgres.io "postgres-12-generated-from-default-1609864589301" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609855369294" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609856085523" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609856466511" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609856836622" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609857659076" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609864032716" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609864589347" deleted
sgpoolingconfig.stackgres.io "generated-from-default-1609864616550" deleted

```

#### Prometheus config maps

The missing part is the postgres-exporter configuration that is stored as a config map:

```bash
## To delete all:
❯ kubectl get configmap -l app=StackGresCluster -o name -n default | xargs kubectl delete
configmap "my-db-cluster-prometheus-postgres-exporter-config" deleted
```


## Operator uninstall

### Using Helm

Execute the steps below to remove the helm install:

```bash
## locate the namespace that the operator was installed
## our doc always points to `stackgres`
❯ helm list --all-namespaces | egrep stackgres-operator\|NAMESPACE
NAME                    NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                           APP VERSION
stackgres-operator      stackgres       1               2021-01-05 10:55:09.543509648 -0300 -03 deployed        stackgres-operator-0.9.3        0.9.3   

## Remove the operator
❯ helm delete stackgres-operator --namespace stackgres
release "stackgres-operator" uninstalled

## ensure that there isn't any object left on the `stackgres` namespace
❯ kubectl get all -n stackgres
No resources found in stackgres namespace.

```

## Manually

This tutorial expects that the operator was installed on the `stackgres` namespace. Change it if you have this installed in a different namespace.

### Operator deployments

Execute the commands below to find and remove the operator deployments:

```bash
## list the deployments in the `stackgres` namespace
❯ kubectl get deployments -n stackgres
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
stackgres-operator   1/1     1            1           171m
stackgres-restapi    1/1     1            1           171m

## delete the deployments in the `stackgres` namespace
❯ kubectl get deployments -n stackgres -o name | xargs kubectl delete -n stackgres
deployment.apps "stackgres-operator" deleted
deployment.apps "stackgres-restapi" deleted
```

### CRDs 

Execute the commands below to find and remove the Custom Resource Definitions (CRDs):

```bash
## list all *.stackgres.io CRDs
❯ kubectl get crds  | egrep stackgres.io\|NAME
NAME                                        CREATED AT
sgobjectstorages.stackgres.io               2021-01-05T13:55:07Z
sgbackups.stackgres.io                      2021-01-05T13:55:07Z
sgclusters.stackgres.io                     2021-01-05T13:55:07Z
sgdistributedlogs.stackgres.io              2021-01-05T13:55:07Z
sginstanceprofiles.stackgres.io             2021-01-05T13:55:07Z
sgpgconfigs.stackgres.io                    2021-01-05T13:55:07Z
sgpoolconfigs.stackgres.io                  2021-01-05T13:55:07Z

## delete the CRDs
❯ kubectl get crds -o name | egrep stackgres.io | xargs kubectl delete
customresourcedefinition.apiextensions.k8s.io "sgobjectstorages.stackgres.io" deleted
customresourcedefinition.apiextensions.k8s.io "sgbackups.stackgres.io" deleted
customresourcedefinition.apiextensions.k8s.io "sgclusters.stackgres.io" deleted
customresourcedefinition.apiextensions.k8s.io "sgdistributedlogs.stackgres.io" deleted
customresourcedefinition.apiextensions.k8s.io "sginstanceprofiles.stackgres.io" deleted
customresourcedefinition.apiextensions.k8s.io "sgpgconfigs.stackgres.io" deleted
customresourcedefinition.apiextensions.k8s.io "sgpoolconfigs.stackgres.io" deleted
```

### Cluster Role Bindings

Execute the commands below to find and remove the Custom Resource Definitions (CRDs):

```bash
## list all
❯ kubectl get clusterrolebinding  |  egrep stackgres\-\|NAME
NAME                                                   AGE
stackgres-operator                                     3h14m
stackgres-restapi                                      3h14m
stackgres-restapi-admin                                3h14m

## delete them
❯ kubectl get clusterrolebinding -o name | grep stackgres- | xargs kubectl delete
clusterrolebinding.rbac.authorization.k8s.io "stackgres-operator" deleted
clusterrolebinding.rbac.authorization.k8s.io "stackgres-restapi" deleted
clusterrolebinding.rbac.authorization.k8s.io "stackgres-restapi-admin" deleted
```

### Cluster Roles

Execute the commands below to find and remove the Custom Resource Definitions (CRDs):

```bash
## list all
❯ kubectl get clusterrole  |  egrep stackgres\-\|NAME
NAME                                                                   AGE
stackgres-operator                                                     3h21m
stackgres-restapi                                                      3h21m

## delete all
❯ kubectl get clusterrole -o name | grep stackgres- | xargs kubectl delete
clusterrole.rbac.authorization.k8s.io "stackgres-operator" deleted
clusterrole.rbac.authorization.k8s.io "stackgres-restapi" deleted
```

### Namespaces

Remove the stackgres namespace:

```bash
kubectl delete namespace stackgres
```

### Other objects

The last items to remove are the `MutatingWebhookConfiguration` and `ValidatingWebhookConfiguration`:

```bash
## to list all
❯ kubectl get mutatingwebhookconfiguration,validatingwebhookconfiguration | egrep stackgres-\|NAME
NAME                                                                                             CREATED AT
mutatingwebhookconfiguration.admissionregistration.k8s.io/stackgres-operator                     2021-01-05T13:55:22Z
NAME                                                                                               CREATED AT
validatingwebhookconfiguration.admissionregistration.k8s.io/stackgres-operator                     2021-01-05T13:55:22Z


## To remove all
❯ kubectl get mutatingwebhookconfiguration,validatingwebhookconfiguration -o name | grep stackgres- | xargs kubectl delete
mutatingwebhookconfiguration.admissionregistration.k8s.io "stackgres-operator" deleted
validatingwebhookconfiguration.admissionregistration.k8s.io "stackgres-operator" deleted
```
