
<!-- --------------------
from architecture
-->

## Operator Availability Concerns

Operator availability only affect the operational plane. The data plane is not affected
at all and the databases will work as expected even when the operator is offline.
The operator is kept available in a best-effort manner. If at some point the operator becomes
unavailable, this can lead to unavailability of following operational aspects:

* Cluster creation / update
* Cluster configuration creation / update / deletion
* Backups generation
* Reconciliation of modified resources controlled by the operator (when
  modified by the user or some other means)

The availability of the operator does not affect the following functional aspects:

* Database high availability
* Connection pooling
* Incremental backups
* Stats collection

<!-- -------------------- -->


<!-- --------------------
from concepts
-->

## StackGres Configuration Definitions

Almost all StackGres configuration is defined in various Kubernetes resource types.
These configuration resources are typically referenced in StackGres clusters.

The StackGres operator ensures that the overall setup is consistent and applies the changes or actions necessary.

The following resource types represent cluster configurations:

- Postgres Configuration (Kubernetes custom resource definition (CRD) `SGPostgresConfig`)
- Pooling Configuration (CRD `SGPoolingConfig`)
- Backup Configuration (CRD `SGBackupConfig`)
- Instance Profile (CRD `SGInstanceProfile`)


## Backup Definitions

Backups are configured by defining a StackGres backup resource (CRD `SGBackup`).
This represents the process of how backups are created.

As it is the case for any other StackGres resource, the backup resource's status field is updated by StackGres and can be inspected.


## Distributed Logs

StackGres supports distributed Postgres logs which are forwarded to and persisted in a separate StackGres cluster.
That is, when distributed logs are configured, which happens with the CRD `SGDistributedLogs`, a separate cluster is created and configured under the hood.

Thus, the distributed log cluster can be queried using SQL as well.


## Database Operations

Database operations are also performed in a declarative way, by creating and configuring resources of type `SGDbOps`.
The database operations are performed by StackGres, and as it is the case for any StackGres resource, their status can be accessed via the usual Kubernetes means, via StackGres REST API, or in the StackGres admin UI.

<!-- -------------------- -->




<!-- --------------------
from stackgres installation
-->

The recommended way to install StackGres is to use the official Helm chart. Additional parameters can be passed to the default installation:
* Access to Grafana. StackGres uses this access to install StackGres specific dashboards as well as to embed Grafana into the web console. If you've installed Prometheus as shown in the previous step, the host and credentials are set to the default values (Grafana service: `prometheus-grafana.monitoring`, username: `admin`, password: `prom-operator`).
* How to expose the web console. You can choose `LoadBalancer` if you're using a Kubernetes setup that supports creating load balancers. Otherwise, you can choose `ClusterIP` (the default), or omit this parameter, in which case you will need to create a custom routing to the console, or use mechanisms such as a port forward, in order to access the web console.

Proceed to install StackGres:

- Add the Helm repo:

```
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
```

- Install the Operator

> StackGres (the operator and associated components) may be installed on any namespace but we recommended to create a dedicated namespace (`stackgres` in this case).

```
helm install --create-namespace --namespace stackgres stackgres-operator \
    --set grafana.autoEmbed=true \
    --set-string grafana.webHost=prometheus-operator-grafana.monitoring \
    --set-string grafana.secretNamespace=monitoring \
    --set-string grafana.secretName=prometheus-operator-grafana \
    --set-string grafana.secretUserKey=admin-user \
    --set-string grafana.secretPasswordKey=admin-password \
    --set-string adminui.service.type=LoadBalancer \
    stackgres-charts/stackgres-operator
```
> You can specify the version to the Helm command. For example you may add `--version 1.0.0` to install verion `1.0.0`.

Note that using `adminui.service.type=LoadBalancer` will create a network load balancer, which may incur in additional costs. You may alternatively use `ClusterIP` if that's your preference.

The StackGres installation may take a few minutes. The output will be similar to:

```
NAME: stackgres-operator
LAST DEPLOYED: Mon Oct 1 00:25:10 2021
NAMESPACE: stackgres
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
Release Name: stackgres-operator
StackGres Version: 1.0.0

   _____ _             _     _____
  / ____| |           | |   / ____|
 | (___ | |_ __ _  ___| | _| |  __ _ __ ___  ___
  \___ \| __/ _` |/ __| |/ / | |_ | '__/ _ \/ __|
  ____) | || (_| | (__|   <| |__| | | |  __/\__ \
 |_____/ \__\__,_|\___|_|\_\\_____|_|  \___||___/
                                  by OnGres, Inc.

Check if the operator was successfully deployed and is available:

    kubectl describe deployment -n stackgres stackgres-operator

    kubectl wait -n stackgres deployment/stackgres-operator --for condition=Available

Check if the restapi was successfully deployed and is available:

    kubectl describe deployment -n stackgres stackgres-restapi

    kubectl wait -n stackgres deployment/stackgres-restapi --for condition=Available

To access StackGres Operator UI from localhost, run the below commands:

    POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")

    kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres

Read more about port forwarding here: http://kubernetes.io/docs/user-guide/kubectl/kubectl_port-forward/

Now you can access the StackGres Operator UI on:

https://localhost:8443

To get the username, run the command:

    kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'

To get the generated password, run the command:

    kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'

Remember to remove the generated password hint from the secret to avoid security flaws:

    kubectl patch secrets --namespace stackgres stackgres-restapi --type json -p '[{"op":"remove","path":"/data/clearPassword"}]'
```

<!-- -------------------- -->

<!-- --------------------
from simple cluster / scaling
-->

## Scaling the cluster

Let's add a new node to the Postgres cluster. Just edit the `simple.yaml` file and change the number of instances from `2` to `3`:

```yaml
  instances: 3
```

and then apply it:

```
$ kubectl apply -f simple.yaml -n stackgres
sgcluster.stackgres.io/simple configured
```

In a few seconds, a third node would have been spinned up:

```
$ kubectl get pods -n stackgres --watch
NAME       READY   STATUS    RESTARTS   AGE
simple-0   6/6     Running   0          55m
simple-1   6/6     Running   0          74m
simple-2   6/6     Running   0          61s
```

And Patroni should also reflect its status as a new replica:

```
$ kubectl exec -it simple-0 -n stackgres -c patroni -- patronictl list
+ Cluster: simple (6979461716096839850) ---+---------+----+-----------+
| Member   | Host                | Role    | State   | TL | Lag in MB |
+----------+---------------------+---------+---------+----+-----------+
| simple-0 | 192.168.40.142:7433 | Leader  | running |  2 |           |
| simple-1 | 192.168.12.150:7433 | Replica | running |  2 |         0 |
| simple-2 | 192.168.71.214:7433 | Replica | running |  2 |         0 |
+----------+---------------------+---------+---------+----+-----------+
```

<!-- -------------------- -->




<!-- --------------------
from tutorial / benchmark
-->

## Running a benchmark

StackGres supports automating "Day 2 Operations".
These operations are performed via a CRD called `SGDbOps`.
`SGDbOps` support several kind of operations, and more will be added in future versions.
Let's try one of them that runs a benchmark.

Create and apply the following YAML file:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: demo
  name: pgbench1
spec:
  op: benchmark
  sgCluster: cluster
  benchmark:
    type: pgbench
    pgbench:
      databaseSize: 1Gi
      duration: P0DT0H2M
      usePreparedStatements: false
      concurrentClients: 20 
      threads: 8 
```

Upon creating this resource, StackGres will schedule and run a benchmark.
The results of the benchmark will be written in the `.Status` field of the CRD, which you can query with `kubectl describe`.
You may also check them from the web console.

<!-- -------------------- -->
