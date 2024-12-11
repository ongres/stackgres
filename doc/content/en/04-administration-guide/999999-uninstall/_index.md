---
title: Uninstall
date:  2021-01-05T10:39:44-03:00
weight: 999999 ## adding a super high value to ensure that this will be the last item
url: /administration/uninstall
description: Details about how to uninstall the operator and all its components.
showToc: true
---

## Uninstalling StackGres custom resources

Assuming that your clusters are running on the `default` namespace, execute the commands below to find and delete the clusters and other StackGres custom resources.

### SGClusters

List the available clusters:

```
kubectl get sgcluster -n default
```

Will show the available SGCluster like in the following output:

```
NAME            AGE
my-db-cluster   4m27s
```

List the pods for the cluster:

```
kubectl get pods -n default
```

```
NAME              READY   STATUS    RESTARTS   AGE
my-db-cluster-0   5/5     Running   1          2m29s
my-db-cluster-1   5/5     Running   1          99s
my-db-cluster-2   5/5     Running   0          74s
```

> **IMPORTANT**: before deleting the cluster make sure all the PersistentVolume are going to be removed
> (if that is your intention). To do so set the field `.spec.persistentVolumeReclaimPolicy` to
> `Delete` for the PersistentVolume that are associated to the PersisitentVolumeClaim of the cluster.
> To find out which are the PersisitentVolume use the the following command:
> 
> ```
> kubectl get pvc -l app=StackGresCluster
> ```

Delete the cluster:

```
kubectl delete sgcluster my-db-cluster -n default
```

```
sgcluster.stackgres.io "my-db-cluster" deleted
```

Check if the pods were deleted:

```
kubectl get pods -n default
```

```
No resources found in default namespace.
```

### SGShardedClusters

List the available clusters:

```
kubectl get sgshardedcluster -n default
```

Will show the available SGShardedCluster like in the following output:

```
NAME            AGE
my-db-cluster   4m27s
```

List the pods for the cluster:

```
kubectl get pods -n default
```

```
NAME                    READY   STATUS    RESTARTS   AGE
my-db-cluster-coord-0   5/5     Running   1          2m29s
my-db-cluster-shard0-0  5/5     Running   1          99s
my-db-cluster-shard1-1  5/5     Running   0          74s
```

> **IMPORTANT**: before deleting the cluster make sure all the PersistentVolume are going to be removed
> (if that is your intention). To do so set the field `.spec.persistentVolumeReclaimPolicy` to
> `Delete` for the PersistentVolume that are associated to the PersisitentVolumeClaim of the cluster.
> To find out which are the PersisitentVolume use the the following command:
> 
> ```
> kubectl get pvc -l app=StackGresShardedCluster
> ```

Delete the cluster:

```
kubectl delete sgshardedcluster my-db-cluster -n default
```

```
sgshardedcluster.stackgres.io "my-db-cluster" deleted
```

Check if the pods were deleted:

```
kubectl get pods -n default
```

```
No resources found in default namespace.
```

### SGDistributedLogs

List the available distributed logs clusters:

```
kubectl get sgdistributedlogs -n default
```

Will show the available SGDistributedLogs like in the following output:

```
NAME            AGE
my-dl-cluster   4m27s
```

List the pods for the cluster:

```
kubectl get pods -n default
```

```
NAME              READY   STATUS    RESTARTS   AGE
my-dl-cluster-0   5/5     Running   1          2m59s
```

> **IMPORTANT**: before deleting the cluster make sure all the PersistentVolume are going to be removed
> (if that is your intention). To do so set the field `.spec.persistentVolumeReclaimPolicy` to
> `Delete` for the PersistentVolume that are associated to the PersisitentVolumeClaim of the cluster.
> To find out which are the PersisitentVolume use the the following command:
> 
> ```
> kubectl get pvc -l app=StackGresCluster
> ```

Delete the distributed logs cluster:

```
kubectl delete sgdistributedlogs my-dl-cluster -n default
```

```
sgdistributedlogs.stackgres.io "my-dl-cluster" deleted
```

Check if the pods were deleted:

```
kubectl get pods -n default
```

```
No resources found in default namespace.
```

### Other StackGres resources

List all StackGres objects:

```
kubectl api-resources -o name | grep -F .stackgres.io \
  | kubectl get "$(tr '\n' ',' | sed 's/,$//')" -n default
```

```
NAME                                                      AGE
sgobjectstorage.stackgres.io/backup-config-minio-backend   162m

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
```

To delete them all:

> **IMPORTANT:** this WILL remove the SGBackups too including the physical backups stored in the
>  VolumeSnapshots (but not the physical backups stored in the object storage).
> **PROCEED WITH CARE.**

```
kubectl api-resources -o name | grep -F .stackgres.io \
  | kubectl delete --all "$(tr '\n' ',' | sed 's/,$//')" -n default
```

```
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

## Uninstall the Operator

See also the section about [uninstalling unamespaced resources](#cleanup-unamespaced-resources)

### When installed with Helm

Execute the steps below to remove the helm chart release:

```
## locate the namespace that the operator was installed
## our doc always points to `stackgres`
helm list --all-namespaces
```

Will show the installed StackGres helm chart releases like in the following output:

```
NAME                    NAMESPACE       REVISION        UPDATED                                 STATUS          CHART                           APP VERSION
stackgres-operator      stackgres       1               2021-01-05 10:55:09.543509648 -0300 -03 deployed        stackgres-operator-0.9.3        0.9.3   
```

Uninstall the operator:

```
helm delete stackgres-operator --namespace stackgres
```

```
release "stackgres-operator" uninstalled
```

Ensure that there isn't any object left on the `stackgres` namespace

```
kubectl get all -n stackgres
```

```
No resources found in stackgres namespace.
```

### When installed with OperatorHub

First delete the Subscription OLM custom resource:

```
kubectl delete subscription -n stackgres stackgres
```

Then delete the ClusterServiceVersion OLM custom resource:

```
kubectl delete clusterserviceversion -n stackgres stackgres.v1.14.1
```

Finally delete the SGConfig StackGres custom resource:

```
kubectl delete sgconfig -n stackgres stackgres
```

### When installed in OpenShift 4.x

```
kubectl delete subscription -n openshift-operators stackgres
```

Then delete the ClusterServiceVersion OLM custom resource:

```
kubectl delete clusterserviceversion -n openshift-operators stackgres.v1.14.1
```

Finally delete the SGConfig StackGres custom resource:

```
kubectl delete sgconfig -n openshift-operators stackgres
```

## Cleanup unamespaced resources

Follow this section in order to remove unamesapced resources.

### Webhooks

StackGres `MutatingWebhookConfiguration` and `ValidatingWebhookConfiguration` are the first thing to remove since they
 will prevent removing CRDs:

List all StackGres MutatingWebhookConfigurations and ValidatingWebhookConfigurations:

```
kubectl get mutatingwebhookconfiguration,validatingwebhookconfiguration | grep -F stackgres-
```

Will show the installed StackGres MutatingWebhookConfigurations and ValidatingWebhookConfigurations like in the following output:

```
mutatingwebhookconfiguration.admissionregistration.k8s.io/stackgres-operator                     2021-01-05T13:55:22Z
validatingwebhookconfiguration.admissionregistration.k8s.io/stackgres-operator                     2021-01-05T13:55:22Z
```

Remove all StackGres MutatingWebhookConfigurations and ValidatingWebhookConfigurations:

```
kubectl get mutatingwebhookconfiguration,validatingwebhookconfiguration -o name | grep stackgres- | xargs kubectl delete
```

```
mutatingwebhookconfiguration.admissionregistration.k8s.io "stackgres-operator" deleted
validatingwebhookconfiguration.admissionregistration.k8s.io "stackgres-operator" deleted
```

## CRDs 

Execute the commands below to find and remove the Custom Resource Definitions (CRDs):

```
## list all *.stackgres.io CRDs
kubectl get crds | grep -F .stackgres.io
```

Will show the installed StackGres CRDs like in the following output:

```
sgobjectstorages.stackgres.io               2021-01-05T13:55:07Z
sgbackups.stackgres.io                      2021-01-05T13:55:07Z
sgclusters.stackgres.io                     2021-01-05T13:55:07Z
sgdistributedlogs.stackgres.io              2021-01-05T13:55:07Z
sginstanceprofiles.stackgres.io             2021-01-05T13:55:07Z
sgpgconfigs.stackgres.io                    2021-01-05T13:55:07Z
sgpoolconfigs.stackgres.io                  2021-01-05T13:55:07Z
```

Delete the StackGres CRDs:

```
kubectl get crds -o name | grep -F .stackgres.io | xargs kubectl delete
```

```
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

List all StackGres ClusterRoleBindings:

```
kubectl get clusterrolebinding | grep -F stackgres-
```

Will show the installed StackGres ClusterRoleBindings like in the following output:

```
stackgres-operator                                     3h14m
stackgres-restapi                                      3h14m
stackgres-restapi-admin                                3h14m
```

Delete the StackGres ClusterRoleBindings:

```
kubectl get clusterrolebinding -o name | grep stackgres- | xargs kubectl delete
```

```
clusterrolebinding.rbac.authorization.k8s.io "stackgres-operator" deleted
clusterrolebinding.rbac.authorization.k8s.io "stackgres-restapi" deleted
clusterrolebinding.rbac.authorization.k8s.io "stackgres-restapi-admin" deleted
```

### Cluster Roles

Execute the commands below to find and remove the Custom Resource Definitions (CRDs):

List all StackGres ClusterRoles:

```
kubectl get clusterrole | grep -F stackgres-
```

Will show the installed StackGres ClusterRoles like in the following output:

```
stackgres-operator                                                     3h21m
stackgres-restapi                                                      3h21m
```

Delete all StackGres ClusterRoles:

```
kubectl get clusterrole -o name | grep stackgres- | xargs kubectl delete
```

```
clusterrole.rbac.authorization.k8s.io "stackgres-operator" deleted
clusterrole.rbac.authorization.k8s.io "stackgres-restapi" deleted
```

### Namespaces

Remove the stackgres namespace:

```
kubectl delete namespace stackgres
```
