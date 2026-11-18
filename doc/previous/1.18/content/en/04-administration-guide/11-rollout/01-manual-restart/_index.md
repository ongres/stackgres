---
title: Manual Cluster Restart
weight: 8
url: /administration/rollout/manual-restart
aliases: [ /install/restart , /install/manual-restart, /administration/manual-restart ]
description: Details about how to restart manually the database nodes.
showToc: true
---

A manual cluster restart can be used after a configuration change that requires a restart (including Postgres, PgBouncer or any configuration of the StackGres cluster).

As a reference, a restart is required when the cluster condition `PendingRestart` inside `.status.conditions` property is `True`, which we can query with the following command:

```
kubectl get sgclusters.stackgres.io -A --template '
{{- range $item := .items }}
  {{- range $item.status.conditions }}
    {{- if eq .type "PendingRestart" }}
      {{- printf "%s.%s %s=%s\n" $item.metadata.namespace $item.metadata.name .type .status }}
    {{- end }}
  {{- end }}
{{- end }}'
```

Example output when a restart is pending:
```
default.my-cluster PendingRestart=True
```

**The restart procedure will generate a service disruption**. The service disruption will start for the
 read write connections when the primary pod is deleted and will end when Patroni elect the new
 primary. For read only connections the service disruption will start when only one replica exists
 and the replica pod is deleted and will end when Patroni set the role of the pod to replica.

There are two restart strategies:

* In-Place Restart: this procedure does not require more resources than those that are available.
 In case only an instance of the StackGres cluster is present this mean the service disruption
 will last longer so we encourage use the reduced impact restart and especially for a production
 environment.
* Reduced Impact Restart: this procedure is the same as the in-place restart but require additional
 resources in order to spawn a new updated replica that will be removed when the procedure completes.

> **NOTE**: If any of postgres's parameters `max_connections`, `max_prepared_transactions`, `max_wal_senders`,
> `max_wal_senders` or `max_locks_per_transaction` are changed to a lower value than they were set
> the primary instance has to be restarted before any replica can be restarted too, the service disruption
> for read write connection will last longer in this case depending how long it take the primary instance
> to restart.

Those procedures includes some shell script snippet examples. In those snippet we assume the
 following environment variables are set with values of the StackGres cluster you want to restart:

```
NAMESPACE=default
SGCLUSTER=example
```

> **NOTE**: If any error arises at any point during restart of a cluster please refer to our [Cluster Restart Troubleshooting section]({{% relref "troubleshooting.md/_index.md" %}})
> to find solutions to common issues or, if [no similar issue exists](https://gitlab.com/ongresinc/stackgres/-/issues?scope=all&utf8=%E2%9C%93&state=all),
> feel free to [open an issue on the StackGres project](https://gitlab.com/ongresinc/stackgres/-/issues/new?issue%5Bassignee_id%5D=&issue%5Bmilestone_id%5D=).

## 1. \[Reduced-impact Restart\] - Add read-only instance

**\[Optional, only for the reduced-impact restart\]**

Edit the `SGCluster` and increment by one the number of instances.

```
INSTANCES="$(kubectl get sgcluster -n "$NAMESPACE" "$SGCLUSTER" --template "{{ .spec.instances }}")"
echo "Increasing cluster instances from $INSTANCES to $((INSTANCES+1))"
kubectl patch sgcluster -n "$NAMESPACE" "$SGCLUSTER" --type merge -p "spec: { instances: $((INSTANCES+1)) }"
```

Wait until the new instance is created and operational, receiving traffic from the Service. This new
 replica has already been initialized with the new components.

```
READ_ONLY_POD="$SGCLUSTER-$INSTANCES"
echo "Waiting for pod $READ_ONLY_POD"
kubectl wait --for=condition=Ready -n "$NAMESPACE" "pod/$READ_ONLY_POD"
while kubectl get pod -n "$NAMESPACE" \
    -l "app=StackGresCluster,stackgres.io/cluster-name=$SGCLUSTER,stackgres.io/cluster=true,role=replica" -o name \
  | grep -F "pod/$READ_ONLY_POD" | wc -l | grep -q 0; do sleep 1; done
```

## 2. \[In-place Restart\] - Restart primary first

**\[Optional, if `max_connections`, `max_prepared_transactions`, `max_wal_senders`,
 `max_wal_senders` or `max_locks_per_transaction` are changed to a lower value than they were set\]**

```
PRIMARY_POD="$(kubectl get pod -n "$NAMESPACE" \
    -l "app=StackGresCluster,stackgres.io/cluster-name=$SGCLUSTER,stackgres.io/cluster=true,role=primary" -o name | head -n 1)"
PRIMARY_POD="${PRIMARY_POD#pod/}"

echo "Restart the primary instance $PRIMARY_POD"
kubectl exec -t -n "$NAMESPACE" "$PRIMARY_POD" -- patronictl restart "$SGCLUSTER" "$PRIMARY_POD" --force

echo "Waiting for the primary pod $PRIMARY_POD"
kubectl wait --for=condition=Ready -n "$NAMESPACE" "pod/$PRIMARY_POD"
```

## 3. \[In-place Restart\] - Check read-only pods to restart

Check which read-only pods requires to be restarted.

```
READ_ONLY_PODS="$([ -z "$READ_ONLY_PODS" ] \
  && kubectl get pod -n "$NAMESPACE" --sort-by '{.metadata.name}' \
    -l "app=StackGresCluster,stackgres.io/cluster-name=$SGCLUSTER,stackgres.io/cluster=true,role=replica" \
    --template '{{ range .items }}{{ printf "%s\n" .metadata.name }}{{ end }}' \
  || (echo "$READ_ONLY_PODS" | tail -n +2))"
echo "Read only pods to restart:"
echo "$READ_ONLY_PODS"
READ_ONLY_POD="$(echo "$READ_ONLY_PODS" | head -n 1)"
[ -z "$READ_ONLY_POD" ] && echo "No more read only pods needs restart" \
  || echo "$READ_ONLY_POD will be restarted"
```

## 4. \[In-place Restart\] - Delete a read-only pod

Delete one of the read-only pods.

```
echo "Deleting read-only pod $READ_ONLY_POD"
kubectl delete -n "$NAMESPACE" pod "$READ_ONLY_POD"
```

A new one will be created, and will also have the new components. Wait until fully operational.

```
echo "Waiting for pod $READ_ONLY_POD"
kubectl wait --for=condition=Ready -n "$NAMESPACE" "pod/$READ_ONLY_POD"
```

## 5. \[In-place Restart\] - Repeat two previous steps

Repeat the previous two steps until no more read-only pods requires restart. In this moment,
 you have a cluster with N+1 instances (pods), all upgraded with the new components except for
 the primary instance.

## 6. \[In-place Restart\] - Perform switchover

If you have at least a read-only pod perform a switchover of the primary pod.

```
READ_ONLY_POD="$(kubectl get pod -n "$NAMESPACE" \
    -l "app=StackGresCluster,stackgres.io/cluster-name=$SGCLUSTER,stackgres.io/cluster=true,role=replica" -o name | head -n 1)"
PRIMARY_POD="$(kubectl get pod -n "$NAMESPACE" \
    -l "app=StackGresCluster,stackgres.io/cluster-name=$SGCLUSTER,stackgres.io/cluster=true,role=primary" -o name | head -n 1)"
READ_ONLY_POD="${READ_ONLY_POD#pod/}"
PRIMARY_POD="${PRIMARY_POD#pod/}"
if [ -n "$READ_ONLY_POD" ]
then
  echo "Performing switchover from primary pod $PRIMARY_POD to read only pod $READ_ONLY_POD"
  [ -n "$PRIMARY_POD" ] \
    && kubectl exec -ti -n "$NAMESPACE" "$PRIMARY_POD" -c patroni -- \
      patronictl switchover --primary "$PRIMARY_POD" --candidate "$READ_ONLY_POD" --force
else
  echo "Can not perform switchover, no read only pod found"
fi
```

## 7. \[In-place Restart\] - Delete the primary pod

Delete the primary pod.


```
if [ -n "$READ_ONLY_POD" ]
then
  echo "Deleting read-only pod $PRIMARY_POD"
else
  echo "Deleting primary pod $PRIMARY_POD"
fi
kubectl delete -n "$NAMESPACE" pod "$PRIMARY_POD"
```

A new read-only (or primary if there were only a single instance) instance will be created. Wait until it is fully operational.

```
echo "Waiting for pod $PRIMARY_POD"
kubectl wait --for=condition=Ready -n "$NAMESPACE" pod "$PRIMARY_POD"
```

## 8. \[Reduced-impact Restart\] - Scale back the cluster size

**\[Optional, only for the small impact procedure\]**

Scale back the cluster size, editing the `SGCluster` and decrementing by one the number of
 instances.

```
INSTANCES="$(kubectl get sgcluster -n "$NAMESPACE" "$SGCLUSTER" --template "{{ .spec.instances }}")"
echo "Decreasing cluster instances from $INSTANCES to $((INSTANCES-1))"
kubectl patch sgcluster -n "$NAMESPACE" "$SGCLUSTER" --type merge -p "spec: { instances: $((INSTANCES-1)) }"
```
