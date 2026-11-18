---
title: SGShardedDbOps
weight: 14
url: /reference/crd/sgshardeddbops
description: Details about SGShardedDbOps
showToc: true
---

___
**Kind:** SGShardedDbOps

**listKind:** SGShardedDbOpsList

**plural:** sgshardeddbops

**singular:** sgshardeddbops

**shortNames** sgsdo
___

The `SGShardedDbOps` custom resource represents database operations that are performed on a Postgres sharded cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGShardedDbOps
metadata:
  name: restart
spec:
  sgShardedCluster: my-cluster
  op: restart
  maxRetries: 1
  restart:
    method: InPlace
```





<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGShardedDbOps</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGShardedDbOps.spec
<sup><sup>[↩ Parent](#sgshardeddbops)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>op</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The kind of operation that will be performed on the SGCluster. Available operations are:

* `resharding`: perform a resharding of the cluster.
* `restart`: perform a restart of the cluster.
* `securityUpgrade`: perform a security upgrade of the cluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgShardedCluster</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of SGShardedCluster on which the operation will be performed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum number of retries the operation is allowed to do after a failure.

A value of `0` (zero) means no retries are made. Defaults to: `0`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecresharding">resharding</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration for resharding. Resharding a sharded cluster is the operation that moves the data among shards in order to try to
 balance the disk space used in each shard. See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecrestart">restart</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of restart
<br/>
        </td>
      </tr>
      <tr>
        <td><b>runAt</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date, that holds UTC scheduled date of the operation execution.

If not specified or if the date it's in the past, it will be interpreted ASAP.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecscheduling">scheduling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod custom node scheduling and affinity configuration<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecsecurityupgrade">securityUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of security upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies a timeout after which the operation execution will be canceled.

If the operation can not be performed due to timeout expiration, the condition `Failed` will have a status of `True` and the reason will be `OperationTimedOut`.

If not specified the operation will never fail for timeout expiration.
<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.spec.resharding
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration for resharding. Resharding a sharded cluster is the operation that moves the data among shards in order to try to
 balance the disk space used in each shard. See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecreshardingcitus">citus</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Citus specific resharding parameters
<br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedDbOps.spec.resharding.citus
<sup><sup>[↩ Parent](#sgshardeddbopsspecresharding)</sup></sup>

Citus specific resharding parameters


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>drainOnly</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          A float number between 0.0 and 1.0 which indicates the maximum difference ratio of node utilization from average utilization.
See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start
<br/>
        </td>
      </tr>
      <tr>
        <td><b>rebalanceStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of a strategy in Rebalancer strategy table. Will pick a default one if not specified
See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start
<br/>
        </td>
      </tr>
      <tr>
        <td><b>threshold</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          A float number between 0.0 and 1.0 which indicates the maximum difference ratio of node utilization from average utilization.
See also https://docs.citusdata.com/en/stable/develop/api_udf.html#citus-rebalance-start
<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.spec.restart
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration of restart


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>method</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The method used to perform the restart operation. Available methods are:

* `InPlace`: the in-place method does not require more resources than those that are available.
  In case only an instance of the StackGres cluster for the coordinator or any shard is present
  this mean the service disruption will last longer so we encourage use the reduced impact restart
   and especially for a production environment.
* `ReducedImpact`: this procedure is the same as the in-place method but require additional
  resources in order to spawn a new updated replica that will be removed when the procedure completes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>onlyPendingRestart</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          By default all Pods are restarted. Setting this option to `true` allow to restart only those Pods which
  are in pending restart state as detected by the operation. Defaults to: `false`.
<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.spec.scheduling
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Pod custom node scheduling and affinity configuration

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinity">nodeAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#nodeaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          NodeSelector is a selector which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinity">podAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#podaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinity">podAntiAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#podantiaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>priorityClassName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If specified, indicates the pod's priority. "system-node-critical" and "system-cluster-critical" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingtolerationsindex">tolerations</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If specified, the pod's tolerations.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedDbOps.spec.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#nodeaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference">preference</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex">nodeSelectorTerms</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Required. A list of node selector terms. The terms are ORed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedDbOps.spec.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#podaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedDbOps.spec.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.33/#podantiaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGShardedDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

#### SGShardedDbOps.spec.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgshardeddbopsspecscheduling)</sup></sup>

The pod this Toleration is attached to tolerates any taint that matches the triple <key,value,effect> using the matching operator <operator>.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>effect</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute.<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator represents a key's relationship to the value. Valid operators are Exists and Equal. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category.<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerationSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.spec.securityUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsspec)</sup></sup>

Configuration of security upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>method</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The method used to perform the security upgrade operation. Available methods are:

* `InPlace`: the in-place method does not require more resources than those that are available.
  In case only an instance of the StackGres cluster is present this mean the service disruption will
  last longer so we encourage use the reduced impact restart and especially for a production environment.
* `ReducedImpact`: this procedure is the same as the in-place method but require additional
  resources in order to spawn a new updated replica that will be removed when the procedure completes.
<br/>
        </td>
      </tr></tbody>
</table>

## SGShardedDbOps.status
<sup><sup>[↩ Parent](#sgshardeddbops)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsstatusconditionsindex">conditions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Possible conditions are:

* Running: to indicate when the operation is actually running
* Completed: to indicate when the operation has completed successfully
* Failed: to indicate when the operation has failed
<br/>
        </td>
      </tr>
      <tr>
        <td><b>opRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of retries performed by the operation
<br/>
        </td>
      </tr>
      <tr>
        <td><b>opStarted</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The ISO 8601 timestamp of when the operation started running
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsstatusrestart">restart</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of a restart
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgshardeddbopsstatussecurityupgrade">securityUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of a security upgrade
<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.status.conditions[index]
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastTransitionTime</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Last time the condition transitioned from one status to another.<br/>
        </td>
      </tr>
      <tr>
        <td><b>message</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A human-readable message indicating details about the transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>reason</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The reason for the condition last transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>status</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Status of the condition, one of `True`, `False` or `Unknown`.<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Type of deployment condition.<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.status.restart
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>

The results of a restart


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A failure message (when available)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingToRestartSgClusters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The SGClusters that are pending to be restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restartedSgClusters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The SGClusters that have been restarted
<br/>
        </td>
      </tr></tbody>
</table>

### SGShardedDbOps.status.securityUpgrade
<sup><sup>[↩ Parent](#sgshardeddbopsstatus)</sup></sup>

The results of a security upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A failure message (when available)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingToRestartSgClusters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The SGClusters that are pending to be restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restartedSgClusters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The SGClusters that have been restarted
<br/>
        </td>
      </tr></tbody>
</table>
