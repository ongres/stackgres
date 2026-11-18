---
title: SGStream
weight: 14
url: /reference/crd/sgstream
description: Details about SGStream
showToc: true
---

___
**Kind:** SGStream

**listKind:** SGStreamList

**plural:** sgstreams

**singular:** sgstream

**shortNames** sgstr
___

The `SGStream` custom resource represents a stream of Change Data Capture (CDC) events from a source database to a target service.

**Example:**

```yaml
apiVersion: stackgres.io/v1alpha1
kind: SGStream
metadata:
  name: cloudevent
spec:
  source:
    type: SGCluster
    sgCluster:
      name: my-cluster
  target:
    type: CloudEvent
    cloudEvent:
      binding: http
      format: json
      http:
        url: cloudevent-service
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
      <td>stackgres.io/v1alpha1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGStream</td>
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
        <td><b><a href="#sgstreamspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Specification of the desired behavior of a StackGres stream.

A stream represent the process of performing a change data capture (CDC) operation on a data source that generates a stream of event containing information about the changes happening (or happened) to the database in real time (or from the beginning).

The stream allow to specify different types for the target of the CDC operation. See `SGStream.spec.target.type`.

The stream perform two distinct operation to generate data source changes for the target:

* Snapshotting: allows to capture the content of the data source in a specific point in time and stream it as if they were changes, thus providing a stream of events as they were an aggregate from the beginning of the existence of the data source.
* Streaming: allows to capture the changes that are happening in real time in the data source and stream them as changes continuously.

The CDC is performed using [Debezium Engine](https://debezium.io/documentation/reference/stable/development/engine.html). SGStream extends functionality of Debezium by providing a [custom signaling channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-custom-signaling-channel) that allow to send signals by simply adding annotation to the SGStream resources.
To send a signal simply create an annotation with the following formar:

```
metadata:
  annotations:
    debezium-signal.stackgres.io/<signal type>: <signal data>
```

Also, SGStream provide the following custom singals implementations:
  
  * `tombstone`: allow to stop completely Debezium streaming and the SGStream. This signal is useful to give an end to the streaming in a graceful way allowing for the removal of the logical slot created by Debezium.
  * `command`: allow to execute any SQL command on the target database. Only available then the target type is `SGCluster`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Status of a StackGres stream.
<br/>
        </td>
      </tr></tbody>
</table>

## SGStream.spec
<sup><sup>[↩ Parent](#sgstream)</sup></sup>

Specification of the desired behavior of a StackGres stream.

A stream represent the process of performing a change data capture (CDC) operation on a data source that generates a stream of event containing information about the changes happening (or happened) to the database in real time (or from the beginning).

The stream allow to specify different types for the target of the CDC operation. See `SGStream.spec.target.type`.

The stream perform two distinct operation to generate data source changes for the target:

* Snapshotting: allows to capture the content of the data source in a specific point in time and stream it as if they were changes, thus providing a stream of events as they were an aggregate from the beginning of the existence of the data source.
* Streaming: allows to capture the changes that are happening in real time in the data source and stream them as changes continuously.

The CDC is performed using [Debezium Engine](https://debezium.io/documentation/reference/stable/development/engine.html). SGStream extends functionality of Debezium by providing a [custom signaling channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-custom-signaling-channel) that allow to send signals by simply adding annotation to the SGStream resources.
To send a signal simply create an annotation with the following formar:

```
metadata:
  annotations:
    debezium-signal.stackgres.io/<signal type>: <signal data>
```

Also, SGStream provide the following custom singals implementations:
  
  * `tombstone`: allow to stop completely Debezium streaming and the SGStream. This signal is useful to give an end to the streaming in a graceful way allowing for the removal of the logical slot created by Debezium.
  * `command`: allow to execute any SQL command on the target database. Only available then the target type is `SGCluster`.


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
        <td><b><a href="#sgstreamspecpods">pods</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The configuration for SGStream Pod<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsource">source</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The data source of the stream to which change data capture will be applied. 
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectarget">target</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The target of this sream.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecdebeziumengineproperties">debeziumEngineProperties</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          See https://debezium.io/documentation/reference/stable/development/engine.html#engine-properties
 Each property is converted from myPropertyName to my.property.name
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
          The maximum number of retries the streaming operation is allowed to do after a failure.

A value of `0` (zero) means no retries are made. A value of `-1` means retries are unlimited. Defaults to: `-1`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>useDebeziumAsyncEngine</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` use Debezium asyncronous engine. See https://debezium.io/blog/2024/07/08/async-embedded-engine/<br/>
          <br/>
            <i>Default</i>: true<br/>
        </td>
      </tr></tbody>
</table>

### SGStream.spec.pods
<sup><sup>[↩ Parent](#sgstreamspec)</sup></sup>

The configuration for SGStream Pod

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
        <td><b><a href="#sgstreamspecpodspersistentvolume">persistentVolume</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod's persistent volume configuration.

<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsresources">resources</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The resources assigned to the stream container.

See https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsscheduling">scheduling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod custom scheduling, affinity and topology spread constratins configuration.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.pods.persistentVolume
<sup><sup>[↩ Parent](#sgstreamspecpods)</sup></sup>

Pod's persistent volume configuration.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  pods:
    persistentVolume:
      size: '5Gi'
      storageClass: default
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
      </tr>
      <tr>
        <td><b>size</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Size of the PersistentVolume for stream Pod. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>storageClass</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of an existing StorageClass in the Kubernetes cluster, used to create the PersistentVolume for stream.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.pods.resources
<sup><sup>[↩ Parent](#sgstreamspecpods)</sup></sup>

The resources assigned to the stream container.

See https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/


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
        <td><b><a href="#sgstreamspecpodsresourcesclaimsindex">claims</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Claims lists the names of resources, defined in spec.resourceClaims, that are used by this container.

This is an alpha field and requires enabling the DynamicResourceAllocation feature gate.

This field is immutable. It can only be set for containers.<br/>
        </td>
      </tr>
      <tr>
        <td><b>limits</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Limits describes the maximum amount of compute resources allowed. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/<br/>
        </td>
      </tr>
      <tr>
        <td><b>requests</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Requests describes the minimum amount of compute resources required. If Requests is omitted for a container, it defaults to Limits if that is explicitly specified, otherwise to an implementation-defined value. Requests cannot exceed Limits. More info: https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.resources.claims[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsresources)</sup></sup>

ResourceClaim references one entry in PodSpec.ResourceClaims.

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
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name must match the name of one entry in pod.spec.resourceClaims of the Pod where this field is used. It makes that resource available inside a container.<br/>
        </td>
      </tr>
      <tr>
        <td><b>request</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Request is the name chosen for a request in the referenced claim. If empty, everything from the claim is made available, otherwise only the result of this request.<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.pods.scheduling
<sup><sup>[↩ Parent](#sgstreamspecpods)</sup></sup>

Pod custom scheduling, affinity and topology spread constratins configuration.


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
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinity">nodeAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#nodeaffinity-v1-core<br/>
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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinity">podAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinity">podAntiAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podantiaffinity-v1-core<br/>
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
        <td><b><a href="#sgstreamspecpodsschedulingtolerationsindex">tolerations</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If specified, the pod's tolerations.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#toleration-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingtopologyspreadconstraintsindex">topologySpreadConstraints</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          TopologySpreadConstraints describes how a group of pods ought to spread across topology domains. Scheduler will schedule pods in a way which abides by the constraints. All topologySpreadConstraints are ANDed.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#topologyspreadconstraint-v1-core<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgstreamspecpodsscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#nodeaffinity-v1-core

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
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinity)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference">preference</a></b></td>
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

##### SGStream.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGStream.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

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

##### SGStream.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinity)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex">nodeSelectorTerms</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Required. A list of node selector terms. The terms are ORed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGStream.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgstreamspecpodsscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podaffinity-v1-core

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinity)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinity)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgstreamspecpodsscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#podantiaffinity-v1-core

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinity)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinity)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both matchLabelKeys and labelSelector. Also, matchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `labelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both mismatchLabelKeys and labelSelector. Also, mismatchLabelKeys cannot be set when labelSelector isn't set. This is a beta field and requires enabling MatchLabelKeysInPodAffinity feature gate (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

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

##### SGStream.spec.pods.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsscheduling)</sup></sup>

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

##### SGStream.spec.pods.scheduling.topologySpreadConstraints[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsscheduling)</sup></sup>

TopologySpreadConstraint specifies how to spread matching pods among the given topology.

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
        <td><b>maxSkew</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          MaxSkew describes the degree to which pods may be unevenly distributed. When `whenUnsatisfiable=DoNotSchedule`, it is the maximum permitted difference between the number of matching pods in the target topology and the global minimum. The global minimum is the minimum number of matching pods in an eligible domain or zero if the number of eligible domains is less than MinDomains. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 2/2/1: In this case, the global minimum is 1. | zone1 | zone2 | zone3 | |  P P  |  P P  |   P   | - if MaxSkew is 1, incoming pod can only be scheduled to zone3 to become 2/2/2; scheduling it onto zone1(zone2) would make the ActualSkew(3-1) on zone1(zone2) violate MaxSkew(1). - if MaxSkew is 2, incoming pod can be scheduled onto any zone. When `whenUnsatisfiable=ScheduleAnyway`, it is used to give higher precedence to topologies that satisfy it. It's a required field. Default value is 1 and 0 is not allowed.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          TopologyKey is the key of node labels. Nodes that have a label with this key and identical values are considered to be in the same topology. We consider each <key, value> as a "bucket", and try to put balanced number of pods into each bucket. We define a domain as a particular instance of a topology. Also, we define an eligible domain as a domain whose nodes meet the requirements of nodeAffinityPolicy and nodeTaintsPolicy. e.g. If TopologyKey is "kubernetes.io/hostname", each Node is a domain of that topology. And, if TopologyKey is "topology.kubernetes.io/zone", each zone is a domain of that topology. It's a required field.<br/>
        </td>
      </tr>
      <tr>
        <td><b>whenUnsatisfiable</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          WhenUnsatisfiable indicates how to deal with a pod if it doesn't satisfy the spread constraint. - DoNotSchedule (default) tells the scheduler not to schedule it. - ScheduleAnyway tells the scheduler to schedule the pod in any location,
  but giving higher precedence to topologies that would help reduce the
  skew.
A constraint is considered "Unsatisfiable" for an incoming pod if and only if every possible node assignment for that pod would violate "MaxSkew" on some topology. For example, in a 3-zone cluster, MaxSkew is set to 1, and pods with the same labelSelector spread as 3/1/1: | zone1 | zone2 | zone3 | | P P P |   P   |   P   | If WhenUnsatisfiable is set to DoNotSchedule, incoming pod can only be scheduled to zone2(zone3) to become 3/2/1(3/1/2) as ActualSkew(2-1) on zone2(zone3) satisfies MaxSkew(1). In other words, the cluster can still be imbalanced, but scheduler won't make it *more* imbalanced. It's a required field.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecpodsschedulingtopologyspreadconstraintsindexlabelselector">labelSelector</a></b></td>
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
          MatchLabelKeys is a set of pod label keys to select the pods over which spreading will be calculated. The keys are used to lookup values from the incoming pod labels, those key-value labels are ANDed with labelSelector to select the group of existing pods over which spreading will be calculated for the incoming pod. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. MatchLabelKeys cannot be set when LabelSelector isn't set. Keys that don't exist in the incoming pod labels will be ignored. A null or empty list means only match against labelSelector.

This is a beta field and requires the MatchLabelKeysInPodTopologySpread feature gate to be enabled (enabled by default).<br/>
        </td>
      </tr>
      <tr>
        <td><b>minDomains</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          MinDomains indicates a minimum number of eligible domains. When the number of eligible domains with matching topology keys is less than minDomains, Pod Topology Spread treats "global minimum" as 0, and then the calculation of Skew is performed. And when the number of eligible domains with matching topology keys equals or greater than minDomains, this value has no effect on scheduling. As a result, when the number of eligible domains is less than minDomains, scheduler won't schedule more than maxSkew Pods to those domains. If value is nil, the constraint behaves as if MinDomains is equal to 1. Valid values are integers greater than 0. When value is not nil, WhenUnsatisfiable must be DoNotSchedule.

For example, in a 3-zone cluster, MaxSkew is set to 2, MinDomains is set to 5 and pods with the same labelSelector spread as 2/2/2: | zone1 | zone2 | zone3 | |  P P  |  P P  |  P P  | The number of domains is less than 5(MinDomains), so "global minimum" is treated as 0. In this situation, new pod with the same labelSelector cannot be scheduled, because computed skew will be 3(3 - 0) if new Pod is scheduled to any of the three zones, it will violate MaxSkew.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeAffinityPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          NodeAffinityPolicy indicates how we will treat Pod's nodeAffinity/nodeSelector when calculating pod topology spread skew. Options are: - Honor: only nodes matching nodeAffinity/nodeSelector are included in the calculations. - Ignore: nodeAffinity/nodeSelector are ignored. All nodes are included in the calculations.

If this value is nil, the behavior is equivalent to the Honor policy. This is a beta-level feature default enabled by the NodeInclusionPolicyInPodTopologySpread feature flag.<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeTaintsPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          NodeTaintsPolicy indicates how we will treat node taints when calculating pod topology spread skew. Options are: - Honor: nodes without taints, along with tainted nodes for which the incoming pod has a toleration, are included. - Ignore: node taints are ignored. All nodes are included.

If this value is nil, the behavior is equivalent to the Ignore policy. This is a beta-level feature default enabled by the NodeInclusionPolicyInPodTopologySpread feature flag.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.pods.scheduling.topologySpreadConstraints[index].labelSelector
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingtopologyspreadconstraintsindex)</sup></sup>

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
        <td><b><a href="#sgstreamspecpodsschedulingtopologyspreadconstraintsindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
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

##### SGStream.spec.pods.scheduling.topologySpreadConstraints[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgstreamspecpodsschedulingtopologyspreadconstraintsindexlabelselector)</sup></sup>

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

### SGStream.spec.source
<sup><sup>[↩ Parent](#sgstreamspec)</sup></sup>

The data source of the stream to which change data capture will be applied. 


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
        <td><b>type</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The type of data source. Available data source types are:

* `SGCluster`: an SGCluster in the same namespace
* `Postgres`: any Postgres instance
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcepostgres">postgres</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The configuration of the data source required when type is `Postgres`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcesgcluster">sgCluster</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The configuration of the data source required when type is `SGCluster`.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.source.postgres
<sup><sup>[↩ Parent](#sgstreamspecsource)</sup></sup>

The configuration of the data source required when type is `Postgres`.


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
        <td><b>host</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The hostname of the Postgres instance.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target database name to which the CDC process will connect to.

If not specified the default postgres database will be targeted.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcepostgresdebeziumproperties">debeziumProperties</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Specific property of the debezium Postgres connector.

See https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-connector-properties

Each property is converted from myPropertyName to my.property.name
<br/>
        </td>
      </tr>
      <tr>
        <td><b>excludes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of regular expressions that allow to match one or more `<schema>.<table>.<column>` entries to be filtered out before sending to the target.

This property is mutually exclusive with `includes`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>includes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of regular expressions that allow to match one or more `<schema>.<table>.<column>` entries to be filtered before sending to the target.

This property is mutually exclusive with `excludes`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcepostgrespassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The password used by the CDC process to connect to the database.

If not specified the default superuser password will be used.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>port</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The port of the Postgres instance. When not specified port 5432 will be used.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcepostgresusername">username</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The username used by the CDC process to connect to the database.

If not specified the default superuser username (by default postgres) will be used.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.source.postgres.debeziumProperties
<sup><sup>[↩ Parent](#sgstreamspecsourcepostgres)</sup></sup>

Specific property of the debezium Postgres connector.

See https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-connector-properties

Each property is converted from myPropertyName to my.property.name


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
        <td><b>binaryHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `bytes`. Specifies how binary (bytea) columns should be represented in change events:

* `bytes` represents binary data as byte array.
* `base64` represents binary data as base64-encoded strings.
* `base64-url-safe` represents binary data as base64-url-safe-encoded strings.
* `hex` represents binary data as hex-encoded (base16) strings.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnMaskHash</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]map[string][]string</td>
        <td>
          An optional section, that allow to specify, for an hash algorithm and a salt, a list of regular expressions that match the fully-qualified names of character-based columns. Fully-qualified names for columns are of the form <schemaName>.<tableName>.<columnName>.
 To match the name of a column Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name. In the resulting change event record, the values for the specified columns are replaced with pseudonyms.
 A pseudonym consists of the hashed value that results from applying the specified hashAlgorithm and salt. Based on the hash function that is used, referential integrity is maintained, while column values are replaced with pseudonyms. Supported hash functions are described in the [MessageDigest section](https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest) of the Java Cryptography Architecture Standard Algorithm Name Documentation.
 In the following example, CzQMA0cB5K is a randomly selected salt.
 columnMaskHash.SHA-256.CzQMA0cB5K=[inventory.orders.customerName,inventory.shipment.customerName]
 If necessary, the pseudonym is automatically shortened to the length of the column. The connector configuration can include multiple properties that specify different hash algorithms and salts.
 Depending on the hash algorithm used, the salt selected, and the actual data set, the resulting data set might not be completely masked.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnMaskHashV2</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]map[string][]string</td>
        <td>
          Similar to also columnMaskHash but using hashing strategy version 2.
 Hashing strategy version 2 should be used to ensure fidelity if the value is being hashed in different places or systems.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnMaskWithLengthChars</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An optional, list of regular expressions that match the fully-qualified names of character-based columns. Set this property if you want the connector to mask the values for a set of columns, for example, if they contain sensitive data. Set length to a positive integer to replace data in the specified columns with the number of asterisk (*) characters specified by the length in the property name. Set length to 0 (zero) to replace data in the specified columns with an empty string.
 The fully-qualified name of a column observes the following format: schemaName.tableName.columnName.
 To match the name of a column, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name.
 You can specify multiple properties with different lengths in a single configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnPropagateSourceType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `[.*]`. An optional, list of regular expressions that match the fully-qualified names of columns for which you want the connector to emit extra parameters that represent column metadata. When this property is set, the connector adds the following fields to the schema of event records:

* `__debezium.source.column.type`
* `__debezium.source.column.length`
* `__debezium.source.column.scale`

These parameters propagate a column’s original type name and length (for variable-width types), respectively.
 Enabling the connector to emit this extra data can assist in properly sizing specific numeric or character-based columns in sink databases.
 The fully-qualified name of a column observes one of the following formats: databaseName.tableName.columnName, or databaseName.schemaName.tableName.columnName.
 To match the name of a column, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnTruncateToLengthChars</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An optional, list of regular expressions that match the fully-qualified names of character-based columns. Set this property if you want to truncate the data in a set of columns when it exceeds the number of characters specified by the length in the property name. Set length to a positive integer value, for example, column.truncate.to.20.chars.
 The fully-qualified name of a column observes the following format: <schemaName>.<tableName>.<columnName>.
 To match the name of a column, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name.
 You can specify multiple properties with different lengths in a single configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>converters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]string</td>
        <td>
          Enumerates a comma-separated list of the symbolic names of the [custom converter](https://debezium.io/documentation/reference/stable/development/converters.html#custom-converters) instances that the connector can use. For example,

```
isbn:
  type: io.debezium.test.IsbnConverter
  schemaName: io.debezium.postgresql.type.Isbn
```

You must set the converters property to enable the connector to use a custom converter.
 For each converter that you configure for a connector, you must also add a .type property, which specifies the fully-qualified name of the class that implements the converter interface.
If you want to further control the behavior of a configured converter, you can add one or more configuration parameters to pass values to the converter. To associate any additional configuration parameter with a converter, prefix the parameter names with the symbolic name of the converter.
 Each property is converted from myPropertyName to my.property.name
<br/>
        </td>
      </tr>
      <tr>
        <td><b>customMetricTags</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          The custom metric tags will accept key-value pairs to customize the MBean object name which should be appended the end of regular name, each key would represent a tag for the MBean object name, and the corresponding value would be the value of that tag the key is. For example:

```
customMetricTags:
  k1: v1
  k2: v2
```
<br/>
        </td>
      </tr>
      <tr>
        <td><b>databaseInitialStatements</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of SQL statements that the connector executes when it establishes a JDBC connection to the database.
 The connector may establish JDBC connections at its own discretion. Consequently, this property is useful for configuration of session parameters only, and not for executing DML statements.
 The connector does not execute these statements when it creates a connection for reading the transaction log.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>datatypePropagateSourceType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `[.*]`. An optional, list of regular expressions that specify the fully-qualified names of data types that are defined for columns in a database. When this property is set, for columns with matching data types, the connector emits event records that include the following extra fields in their schema:

* `__debezium.source.column.type`
* `__debezium.source.column.length`
* `__debezium.source.column.scale`

These parameters propagate a column’s original type name and length (for variable-width types), respectively.
 Enabling the connector to emit this extra data can assist in properly sizing specific numeric or character-based columns in sink databases.
 The fully-qualified name of a column observes one of the following formats: databaseName.tableName.typeName, or databaseName.schemaName.tableName.typeName.
 To match the name of a data type, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the data type; the expression does not match substrings that might be present in a type name.
 For the list of PostgreSQL-specific data type names, see the [PostgreSQL data type mappings](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-data-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>decimalHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `precise`. Specifies how the connector should handle values for DECIMAL and NUMERIC columns:

* `precise`: represents values by using java.math.BigDecimal to represent values in binary form in change events.
* `double`: represents values by using double values, which might result in a loss of precision but which is easier to use.
* `string`: encodes values as formatted strings, which are easy to consume but semantic information about the real type is lost. For more information, see [Decimal types](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-decimal-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>errorsMaxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `-1`. Specifies how the connector responds after an operation that results in a retriable error, such as a connection error.

Set one of the following options:

* `-1`: No limit. The connector always restarts automatically, and retries the operation, regardless of the number of previous failures.
* `0`: Disabled. The connector fails immediately, and never retries the operation. User intervention is required to restart the connector.
* `> 0`: The connector restarts automatically until it reaches the specified maximum number of retries. After the next failure, the connector stops, and user intervention is required to restart it.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>eventProcessingFailureHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `fail`. Specifies how the connector should react to exceptions during processing of events:

* `fail`: propagates the exception, indicates the offset of the problematic event, and causes the connector to stop.
* `warn`: logs the offset of the problematic event, skips that event, and continues processing.
* `skip`: skips the problematic event and continues processing.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>fieldNameAdjustmentMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `none`. Specifies how field names should be adjusted for compatibility with the message converter used by the connector. Possible settings:

* `none` does not apply any adjustment.
* `avro` replaces the characters that cannot be used in the Avro type name with underscore.
* `avro_unicode` replaces the underscore or characters that cannot be used in the Avro type name with corresponding unicode like _uxxxx. Note: _ is an escape sequence like backslash in Java

For more information, see [Avro naming](https://debezium.io/documentation/reference/stable/configuration/avro.html#avro-naming).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>flushLsnSource</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Determines whether the connector should commit the LSN of the processed records in the source postgres database so that the WAL logs can be deleted. Specify false if you don’t want the connector to do this. Please note that if set to false LSN will not be acknowledged by Debezium and as a result WAL logs will not be cleared which might result in disk space issues. User is expected to handle the acknowledgement of LSN outside Debezium.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>heartbeatActionQuery</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Specifies a query that the connector executes on the source database when the connector sends a heartbeat message.
 This is useful for resolving the situation described in [WAL disk space consumption](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-wal-disk-space), where capturing changes from a low-traffic database on the same host as a high-traffic database prevents Debezium from processing WAL records and thus acknowledging WAL positions with the database. To address this situation, create a heartbeat table in the low-traffic database, and set this property to a statement that inserts records into that table, for example:

 ```
 INSERT INTO test_heartbeat_table (text) VALUES ('test_heartbeat')
 ```
 
 This allows the connector to receive changes from the low-traffic database and acknowledge their LSNs, which prevents unbounded WAL growth on the database host.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>heartbeatIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `0`. Controls how frequently the connector sends heartbeat messages to a Kafka topic. The default behavior is that the connector does not send heartbeat messages.
 Heartbeat messages are useful for monitoring whether the connector is receiving change events from the database. Heartbeat messages might help decrease the number of change events that need to be re-sent when a connector restarts. To send heartbeat messages, set this property to a positive integer, which indicates the number of milliseconds between heartbeat messages.
 Heartbeat messages are needed when there are many updates in a database that is being tracked but only a tiny number of updates are related to the table(s) and schema(s) for which the connector is capturing changes. In this situation, the connector reads from the database transaction log as usual but rarely emits change records to Kafka. This means that no offset updates are committed to Kafka and the connector does not have an opportunity to send the latest retrieved LSN to the database. The database retains WAL files that contain events that have already been processed by the connector. Sending heartbeat messages enables the connector to send the latest retrieved LSN to the database, which allows the database to reclaim disk space being used by no longer needed WAL files.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hstoreHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `json`. Specifies how the connector should handle values for hstore columns:

* `map`: represents values by using MAP.
* `json`: represents values by using json string. This setting encodes values as formatted strings such as {"key" : "val"}. For more information, see [PostgreSQL HSTORE type](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-hstore-type).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>includeUnknownDatatypes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Specifies connector behavior when the connector encounters a field whose data type is unknown. The default behavior is that the connector omits the field from the change event and logs a warning.
 Set this property to true if you want the change event to contain an opaque binary representation of the field. This lets consumers decode the field. You can control the exact representation by setting the binaryHandlingMode property.
> *NOTE*: Consumers risk backward compatibility issues when `includeUnknownDatatypes` is set to `true`. Not only may the database-specific binary representation change between releases, but if the data type is eventually supported by Debezium, the data type will be sent downstream in a logical type, which would require adjustments by consumers. In general, when encountering unsupported data types, create a feature request so that support can be added.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>incrementalSnapshotChunkSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `1024`. The maximum number of rows that the connector fetches and reads into memory during an incremental snapshot chunk. Increasing the chunk size provides greater efficiency, because the snapshot runs fewer snapshot queries of a greater size. However, larger chunk sizes also require more memory to buffer the snapshot data. Adjust the chunk size to a value that provides the best performance in your environment.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>incrementalSnapshotWatermarkingStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `insert_insert`. Specifies the watermarking mechanism that the connector uses during an incremental snapshot to deduplicate events that might be captured by an incremental snapshot and then recaptured after streaming resumes.

You can specify one of the following options:

* `insert_insert`: When you send a signal to initiate an incremental snapshot, for every chunk that Debezium reads during the snapshot, it writes an entry to the signaling data collection to record the signal to open the snapshot window. After the snapshot completes, Debezium inserts a second entry to record the closing of the window.
* `insert_delete`: When you send a signal to initiate an incremental snapshot, for every chunk that Debezium reads, it writes a single entry to the signaling data collection to record the signal to open the snapshot window. After the snapshot completes, this entry is removed. No entry is created for the signal to close the snapshot window. Set this option to prevent rapid growth of the signaling data collection.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>intervalHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `numeric`. Specifies how the connector should handle values for interval columns:

 * `numeric`: represents intervals using approximate number of microseconds.
 * `string`: represents intervals exactly by using the string pattern representation P<years>Y<months>M<days>DT<hours>H<minutes>M<seconds>S. For example: P1Y2M3DT4H5M6.78S. For more information, see [PostgreSQL basic types](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-basic-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxBatchSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `2048`. Positive integer value that specifies the maximum size of each batch of events that the connector processes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxQueueSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `8192`. Positive integer value that specifies the maximum number of records that the blocking queue can hold. When Debezium reads events streamed from the database, it places the events in the blocking queue before it writes them to Kafka. The blocking queue can provide backpressure for reading change events from the database in cases where the connector ingests messages faster than it can write them to Kafka, or when Kafka becomes unavailable. Events that are held in the queue are disregarded when the connector periodically records offsets. Always set the value of maxQueueSize to be larger than the value of maxBatchSize.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxQueueSizeInBytes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `0`. A long integer value that specifies the maximum volume of the blocking queue in bytes. By default, volume limits are not specified for the blocking queue. To specify the number of bytes that the queue can consume, set this property to a positive long value.
 If maxQueueSize is also set, writing to the queue is blocked when the size of the queue reaches the limit specified by either property. For example, if you set maxQueueSize=1000, and maxQueueSizeInBytes=5000, writing to the queue is blocked after the queue contains 1000 records, or after the volume of the records in the queue reaches 5000 bytes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>messageKeyColumns</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of expressions that specify the columns that the connector uses to form custom message keys for change event records that it publishes to the Kafka topics for specified tables.
 By default, Debezium uses the primary key column of a table as the message key for records that it emits. In place of the default, or to specify a key for tables that lack a primary key, you can configure custom message keys based on one or more columns.
 To establish a custom message key for a table, list the table, followed by the columns to use as the message key. Each list entry takes the following format:
 <fully-qualified_tableName>:<keyColumn>,<keyColumn>
 To base a table key on multiple column names, insert commas between the column names.
 Each fully-qualified table name is a regular expression in the following format:
 <schemaName>.<tableName>
 The property can include entries for multiple tables. Use a semicolon to separate table entries in the list.
 The following example sets the message key for the tables inventory.customers and purchase.orders:
 inventory.customers:pk1,pk2;(.*).purchaseorders:pk3,pk4
 For the table inventory.customer, the columns pk1 and pk2 are specified as the message key. For the purchaseorders tables in any schema, the columns pk3 and pk4 server as the message key.
 There is no limit to the number of columns that you use to create custom message keys. However, it’s best to use the minimum number that are required to specify a unique key.
 Note that having this property set and REPLICA IDENTITY set to DEFAULT on the tables, will cause the tombstone events to not be created properly if the key columns are not part of the primary key of the table. Setting REPLICA IDENTITY to FULL is the only solution.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>moneyFractionDigits</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `2`. Specifies how many decimal digits should be used when converting Postgres money type to java.math.BigDecimal, which represents the values in change events. Applicable only when decimalHandlingMode is set to precise.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>notificationEnabledChannels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          List of notification channel names that are enabled for the connector. By default, the following channels are available: sink, log and jmx. Optionally, you can also implement a [custom notification channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-enabling-custom-signaling-channel).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pluginName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `pgoutput`. The name of the [PostgreSQL logical decoding plug-in](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-output-plugin) installed on the PostgreSQL server. Supported values are decoderbufs, and pgoutput.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pollIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `500`. Positive integer value that specifies the number of milliseconds the connector should wait for new change events to appear before it starts processing a batch of events. Defaults to 500 milliseconds.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>provideTransactionMetadata</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. Determines whether the connector generates events with transaction boundaries and enriches change event envelopes with transaction metadata. Specify true if you want the connector to do this. For more information, see [Transaction metadata](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-transaction-metadata).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>publicationAutocreateMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `all_tables`. Applies only when streaming changes by using [the pgoutput plug-in](https://www.postgresql.org/docs/current/sql-createpublication.html). The setting determines how creation of a [publication](https://www.postgresql.org/docs/current/logical-replication-publication.html) should work. Specify one of the following values:

* `all_tables` - If a publication exists, the connector uses it. If a publication does not exist, the connector creates a publication for all tables in the database for which the connector is capturing changes. For the connector to create a publication it must access the database through a database user account that has permission to create publications and perform replications. You grant the required permission by using the following SQL command CREATE PUBLICATION <publication_name> FOR ALL TABLES;.
* `disabled` - The connector does not attempt to create a publication. A database administrator or the user configured to perform replications must have created the publication before running the connector. If the connector cannot find the publication, the connector throws an exception and stops.
* `filtered` - If a publication exists, the connector uses it. If no publication exists, the connector creates a new publication for tables that match the current filter configuration as specified by the schema.include.list, schema.exclude.list, and table.include.list, and table.exclude.list connector configuration properties. For example: CREATE PUBLICATION <publication_name> FOR TABLE <tbl1, tbl2, tbl3>. If the publication exists, the connector updates the publication for tables that match the current filter configuration. For example: ALTER PUBLICATION <publication_name> SET TABLE <tbl1, tbl2, tbl3>.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>publicationName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default <SGStream name>.<SGStream namespace> (with all characters that are not `[a-zA-Z0-9]` changed to `_` character). The name of the PostgreSQL publication created for streaming changes when using pgoutput. This publication is created at start-up if it does not already exist and it includes all tables. Debezium then applies its own include/exclude list filtering, if configured, to limit the publication to change events for the specific tables of interest. The connector user must have superuser permissions to create this publication, so it is usually preferable to create the publication before starting the connector for the first time. If the publication already exists, either for all tables or configured with a subset of tables, Debezium uses the publication as it is defined.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicaIdentityAutosetValues</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The setting determines the value for [replica identity](https://www.postgresql.org/docs/current/sql-altertable.html#SQL-ALTERTABLE-REPLICA-IDENTITY) at table level.
  This option will overwrite the existing value in database. A comma-separated list of regular expressions that match fully-qualified tables and replica identity value to be used in the table.
  Each expression must match the pattern '<fully-qualified table name>:<replica identity>', where the table name could be defined as (SCHEMA_NAME.TABLE_NAME), and the replica identity values are:
  DEFAULT - Records the old values of the columns of the primary key, if any. This is the default for non-system tables.
  INDEX index_name - Records the old values of the columns covered by the named index, that must be unique, not partial, not deferrable, and include only columns marked NOT NULL. If this index is dropped, the behavior is the same as NOTHING.
  FULL - Records the old values of all columns in the row.
  NOTHING - Records no information about the old row. This is the default for system tables.
  For example,
  schema1.*:FULL,schema2.table2:NOTHING,schema2.table3:INDEX idx_name
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retriableRestartConnectorWaitMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000` (10 seconds). The number of milliseconds to wait before restarting a connector after a retriable error occurs.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>schemaNameAdjustmentMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `none`. Specifies how schema names should be adjusted for compatibility with the message converter used by the connector. Possible settings:

* `none` does not apply any adjustment.
* `avro` replaces the characters that cannot be used in the Avro type name with underscore.
* `avro_unicode` replaces the underscore or characters that cannot be used in the Avro type name with corresponding unicode like _uxxxx. Note: _ is an escape sequence like backslash in Java
<br/>
        </td>
      </tr>
      <tr>
        <td><b>schemaRefreshMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `columns_diff`. Specify the conditions that trigger a refresh of the in-memory schema for a table.

* `columns_diff`: is the safest mode. It ensures that the in-memory schema stays in sync with the database table’s schema at all times.
* `columns_diff_exclude_unchanged_toast`: instructs the connector to refresh the in-memory schema cache if there is a discrepancy with the schema derived from the incoming message, unless unchanged TOASTable data fully accounts for the discrepancy.

This setting can significantly improve connector performance if there are frequently-updated tables that have TOASTed data that are rarely part of updates. However, it is possible for the in-memory schema to become outdated if TOASTable columns are dropped from the table.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>signalDataCollection</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Fully-qualified name of the data collection that is used to send signals to the connector. Use the following format to specify the collection name: <schemaName>.<tableName>
<br/>
        </td>
      </tr>
      <tr>
        <td><b>signalEnabledChannels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `[sgstream-annotations]`. List of the signaling channel names that are enabled for the connector. By default, the following channels are available: sgstream-annotations, source, kafka, file and jmx. Optionally, you can also implement a [custom signaling channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-enabling-custom-signaling-channel).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipMessagesWithoutChange</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. Specifies whether to skip publishing messages when there is no change in included columns. This would essentially filter messages if there is no change in columns included as per includes or excludes fields. Note: Only works when REPLICA IDENTITY of the table is set to FULL
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skippedOperations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `none`. A list of operation types that will be skipped during streaming. The operations include: c for inserts/create, u for updates, d for deletes, t for truncates, and none to not skip any operations. By default, no operations are skipped.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotDropOnStop</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Whether or not to delete the logical replication slot when the connector stops in a graceful, expected way. The default behavior is that the replication slot remains configured for the connector when the connector stops. When the connector restarts, having the same replication slot enables the connector to start processing where it left off. Set to true in only testing or development environments. Dropping the slot allows the database to discard WAL segments. When the connector restarts it performs a new snapshot or it can continue from a persistent offset in the Kafka Connect offsets topic.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotMaxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `6`. If connecting to a replication slot fails, this is the maximum number of consecutive attempts to connect.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default <SGStream namespace>.<SGStream name> (with all characters that are not `[a-zA-Z0-9]` changed to `_` character). The name of the PostgreSQL logical decoding slot that was created for streaming changes from a particular plug-in for a particular database/schema. The server uses this slot to stream events to the Debezium connector that you are configuring.

Slot names must conform to [PostgreSQL replication slot naming rules](https://www.postgresql.org/docs/current/static/warm-standby.html#STREAMING-REPLICATION-SLOTS-MANIPULATION), which state: "Each replication slot has a name, which can contain lower-case letters, numbers, and the underscore character."
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotRetryDelayMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000` (10 seconds). The number of milliseconds to wait between retry attempts when the connector fails to connect to a replication slot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotStreamParams</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Parameters to pass to the configured logical decoding plug-in. For example:

```
slotStreamParams:
  add-tables: "public.table,public.table2"
  include-lsn: "true"
```
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotDelayMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          An interval in milliseconds that the connector should wait before performing a snapshot when the connector starts. If you are starting multiple connectors in a cluster, this property is useful for avoiding snapshot interruptions, which might cause re-balancing of connectors.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotFetchSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10240`. During a snapshot, the connector reads table content in batches of rows. This property specifies the maximum number of rows in a batch.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotIncludeCollectionList</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default <All tables / All tables filtered in `includes` field / All tables that are not filtered out in `excludes` field>. An optional, list of regular expressions that match the fully-qualified names (<schemaName>.<tableName>) of the tables to include in a snapshot. The specified items must be named in the connector’s table.include.list property. This property takes effect only if the connector’s snapshotMode property is set to a value other than `never`. This property does not affect the behavior of incremental snapshots.
   To match the name of a table, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the table; it does not match substrings that might be present in a table name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotLockTimeoutMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. Positive integer value that specifies the maximum amount of time (in milliseconds) to wait to obtain table locks when performing a snapshot. If the connector cannot acquire table locks in this time interval, the snapshot fails. [How the connector performs snapshots](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-snapshots) provides details.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotLockingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `none`. Specifies how the connector holds locks on tables while performing a schema snapshot. Set one of the following options:

* `shared`: The connector holds a table lock that prevents exclusive table access during the initial portion phase of the snapshot in which database schemas and other metadata are read. After the initial phase, the snapshot no longer requires table locks.
* `none`: The connector avoids locks entirely. Do not use this mode if schema changes might occur during the snapshot.

> *WARNING*: Do not use this mode if schema changes might occur during the snapshot.

* `custom`: The connector performs a snapshot according to the implementation specified by the snapshotLockingModeCustomName property, which is a custom implementation of the io.debezium.spi.snapshot.SnapshotLock interface.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotLockingModeCustomName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When snapshotLockingMode is set to custom, use this setting to specify the name of the custom implementation provided in the name() method that is defined by the 'io.debezium.spi.snapshot.SnapshotLock' interface. For more information, see [custom snapshotter SPI](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#connector-custom-snapshot).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotMaxThreads</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `1`. Specifies the number of threads that the connector uses when performing an initial snapshot. To enable parallel initial snapshots, set the property to a value greater than 1. In a parallel initial snapshot, the connector processes multiple tables concurrently. This feature is incubating.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `initial`. Specifies the criteria for performing a snapshot when the connector starts:

* `always` - The connector performs a snapshot every time that it starts. The snapshot includes the structure and data of the captured tables. Specify this value to populate topics with a complete representation of the data from the captured tables every time that the connector starts. After the snapshot completes, the connector begins to stream event records for subsequent database changes.
* `initial` - The connector performs a snapshot only when no offsets have been recorded for the logical server name.
* `initial_only` - The connector performs an initial snapshot and then stops, without processing any subsequent changes.
* `no_data` - The connector never performs snapshots. When a connector is configured this way, after it starts, it behaves as follows: If there is a previously stored LSN in the Kafka offsets topic, the connector continues streaming changes from that position. If no LSN is stored, the connector starts streaming changes from the point in time when the PostgreSQL logical replication slot was created on the server. Use this snapshot mode only when you know all data of interest is still reflected in the WAL.
* `never` - Deprecated see no_data.
* `when_needed` - After the connector starts, it performs a snapshot only if it detects one of the following circumstances: 
  It cannot detect any topic offsets.
  A previously recorded offset specifies a log position that is not available on the server.
* `configuration_based` - With this option, you control snapshot behavior through a set of connector properties that have the prefix 'snapshotModeConfigurationBased'.
* `custom` - The connector performs a snapshot according to the implementation specified by the snapshotModeCustomName property, which defines a custom implementation of the io.debezium.spi.snapshot.Snapshotter interface.

For more information, see the [table of snapshot.mode options](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-connector-snapshot-mode-options).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotData</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector includes table data when it performs a snapshot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotOnDataError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, this property specifies whether the connector attempts to snapshot table data if it does not find the last committed offset in the transaction log. Set the value to true to instruct the connector to perform a new snapshot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotOnSchemaError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector includes table schema in a snapshot if the schema history topic is not available.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotSchema</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector includes the table schema when it performs a snapshot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedStartStream</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector begins to stream change events after a snapshot completes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeCustomName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When snapshotMode is set as custom, use this setting to specify the name of the custom implementation provided in the name() method that is defined by the 'io.debezium.spi.snapshot.Snapshotter' interface. The provided implementation is called after a connector restart to determine whether to perform a snapshot. For more information, see [custom snapshotter SPI](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#connector-custom-snapshot).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotQueryMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `select_all`. Specifies how the connector queries data while performing a snapshot. Set one of the following options:

* `select_all`: The connector performs a select all query by default, optionally adjusting the columns selected based on the column include and exclude list configurations.
* `custom`: The connector performs a snapshot query according to the implementation specified by the snapshotQueryModeCustomName property, which defines a custom implementation of the io.debezium.spi.snapshot.SnapshotQuery interface. This setting enables you to manage snapshot content in a more flexible manner compared to using the snapshotSelectStatementOverrides property.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotQueryModeCustomName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When snapshotQueryMode is set as custom, use this setting to specify the name of the custom implementation provided in the name() method that is defined by the 'io.debezium.spi.snapshot.SnapshotQuery' interface. For more information, see [custom snapshotter SPI](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#connector-custom-snapshot).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotSelectStatementOverrides</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Specifies the table rows to include in a snapshot. Use the property if you want a snapshot to include only a subset of the rows in a table. This property affects snapshots only. It does not apply to events that the connector reads from the log.
 The property contains a hierarchy of fully-qualified table names in the form <schemaName>.<tableName>. For example,

```
snapshotSelectStatementOverrides: 
  "customers.orders": "SELECT * FROM [customers].[orders] WHERE delete_flag = 0 ORDER BY id DESC"
```

In the resulting snapshot, the connector includes only the records for which delete_flag = 0.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>statusUpdateIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. Frequency for sending replication connection status updates to the server, given in milliseconds. The property also controls how frequently the database status is checked to detect a dead connection in case the database was shut down.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timePrecisionMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `adaptive`. Time, date, and timestamps can be represented with different kinds of precision:

* `adaptive`: captures the time and timestamp values exactly as in the database using either millisecond, microsecond, or nanosecond precision values based on the database column’s type.
* `adaptive_time_microseconds`: captures the date, datetime and timestamp values exactly as in the database using either millisecond, microsecond, or nanosecond precision values based on the database column’s type. An exception is TIME type fields, which are always captured as microseconds.
* `connect`: always represents time and timestamp values by using Kafka Connect’s built-in representations for Time, Date, and Timestamp, which use millisecond precision regardless of the database columns' precision. For more information, see [temporal values](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-temporal-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tombstonesOnDelete</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Controls whether a delete event is followed by a tombstone event.

* `true` - a delete operation is represented by a delete event and a subsequent tombstone event.
* `false` - only a delete event is emitted.

After a source record is deleted, emitting a tombstone event (the default behavior) allows Kafka to completely delete all events that pertain to the key of the deleted row in case [log compaction](https://kafka.apache.org/documentation/#compaction) is enabled for the topic.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicCacheSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. The size used for holding the topic names in bounded concurrent hash map. This cache will help to determine the topic name corresponding to a given data collection.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicDelimiter</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `.`. Specify the delimiter for topic name, defaults to ".".
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicHeartbeatPrefix</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `__debezium-heartbeat`. Controls the name of the topic to which the connector sends heartbeat messages. For example, if the topic prefix is fulfillment, the default topic name is __debezium-heartbeat.fulfillment.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicNamingStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `io.debezium.schema.SchemaTopicNamingStrategy`. The name of the TopicNamingStrategy class that should be used to determine the topic name for data change, schema change, transaction, heartbeat event etc., defaults to SchemaTopicNamingStrategy.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicTransaction</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `transaction`. Controls the name of the topic to which the connector sends transaction metadata messages. For example, if the topic prefix is fulfillment, the default topic name is fulfillment.transaction.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unavailableValuePlaceholder</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `__debezium_unavailable_value`. Specifies the constant that the connector provides to indicate that the original value is a toasted value that is not provided by the database. If the setting of unavailable.value.placeholder starts with the hex: prefix it is expected that the rest of the string represents hexadecimally encoded octets. For more information, see [toasted values](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-toasted-values).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>xminFetchIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `0`. How often, in milliseconds, the XMIN will be read from the replication slot. The XMIN value provides the lower bounds of where a new replication slot could start from. The default value of 0 disables tracking XMIN tracking.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.source.postgres.password
<sup><sup>[↩ Parent](#sgstreamspecsourcepostgres)</sup></sup>

The password used by the CDC process to connect to the database.

If not specified the default superuser password will be used.


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
          The Secret key where the password is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the password is stored.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.source.postgres.username
<sup><sup>[↩ Parent](#sgstreamspecsourcepostgres)</sup></sup>

The username used by the CDC process to connect to the database.

If not specified the default superuser username (by default postgres) will be used.


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
          The Secret key where the username is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the username is stored.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.source.sgCluster
<sup><sup>[↩ Parent](#sgstreamspecsource)</sup></sup>

The configuration of the data source required when type is `SGCluster`.


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
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target SGCluster name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target database name to which the CDC process will connect to.

If not specified the default postgres database will be targeted.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcesgclusterdebeziumproperties">debeziumProperties</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Specific property of the debezium Postgres connector.

See https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-connector-properties

Each property is converted from myPropertyName to my.property.name
<br/>
        </td>
      </tr>
      <tr>
        <td><b>excludes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of regular expressions that allow to match one or more `<schema>.<table>.<column>` entries to be filtered out before sending to the target.

This property is mutually exclusive with `includes`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>includes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of regular expressions that allow to match one or more `<schema>.<table>.<column>` entries to be filtered before sending to the target.

This property is mutually exclusive with `excludes`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcesgclusterpassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The password used by the CDC process to connect to the database.

If not specified the default superuser password will be used.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipDropReplicationSlotAndPublicationOnTombstone</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` replication slot and publication will not be dropped after receiving the tombstone signal.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspecsourcesgclusterusername">username</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The username used by the CDC process to connect to the database.

If not specified the default superuser username (by default postgres) will be used.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.source.sgCluster.debeziumProperties
<sup><sup>[↩ Parent](#sgstreamspecsourcesgcluster)</sup></sup>

Specific property of the debezium Postgres connector.

See https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-connector-properties

Each property is converted from myPropertyName to my.property.name


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
        <td><b>binaryHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `bytes`. Specifies how binary (bytea) columns should be represented in change events:

* `bytes` represents binary data as byte array.
* `base64` represents binary data as base64-encoded strings.
* `base64-url-safe` represents binary data as base64-url-safe-encoded strings.
* `hex` represents binary data as hex-encoded (base16) strings.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnMaskHash</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]map[string][]string</td>
        <td>
          An optional section, that allow to specify, for an hash algorithm and a salt, a list of regular expressions that match the fully-qualified names of character-based columns. Fully-qualified names for columns are of the form <schemaName>.<tableName>.<columnName>.
 To match the name of a column Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name. In the resulting change event record, the values for the specified columns are replaced with pseudonyms.
 A pseudonym consists of the hashed value that results from applying the specified hashAlgorithm and salt. Based on the hash function that is used, referential integrity is maintained, while column values are replaced with pseudonyms. Supported hash functions are described in the [MessageDigest section](https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest) of the Java Cryptography Architecture Standard Algorithm Name Documentation.
 In the following example, CzQMA0cB5K is a randomly selected salt.
 columnMaskHash.SHA-256.CzQMA0cB5K=[inventory.orders.customerName,inventory.shipment.customerName]
 If necessary, the pseudonym is automatically shortened to the length of the column. The connector configuration can include multiple properties that specify different hash algorithms and salts.
 Depending on the hash algorithm used, the salt selected, and the actual data set, the resulting data set might not be completely masked.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnMaskHashV2</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]map[string][]string</td>
        <td>
          Similar to also columnMaskHash but using hashing strategy version 2.
 Hashing strategy version 2 should be used to ensure fidelity if the value is being hashed in different places or systems.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnMaskWithLengthChars</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An optional, list of regular expressions that match the fully-qualified names of character-based columns. Set this property if you want the connector to mask the values for a set of columns, for example, if they contain sensitive data. Set length to a positive integer to replace data in the specified columns with the number of asterisk (*) characters specified by the length in the property name. Set length to 0 (zero) to replace data in the specified columns with an empty string.
 The fully-qualified name of a column observes the following format: schemaName.tableName.columnName.
 To match the name of a column, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name.
 You can specify multiple properties with different lengths in a single configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnPropagateSourceType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `[.*]`. An optional, list of regular expressions that match the fully-qualified names of columns for which you want the connector to emit extra parameters that represent column metadata. When this property is set, the connector adds the following fields to the schema of event records:

* `__debezium.source.column.type`
* `__debezium.source.column.length`
* `__debezium.source.column.scale`

These parameters propagate a column’s original type name and length (for variable-width types), respectively.
 Enabling the connector to emit this extra data can assist in properly sizing specific numeric or character-based columns in sink databases.
 The fully-qualified name of a column observes one of the following formats: databaseName.tableName.columnName, or databaseName.schemaName.tableName.columnName.
 To match the name of a column, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnTruncateToLengthChars</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An optional, list of regular expressions that match the fully-qualified names of character-based columns. Set this property if you want to truncate the data in a set of columns when it exceeds the number of characters specified by the length in the property name. Set length to a positive integer value, for example, column.truncate.to.20.chars.
 The fully-qualified name of a column observes the following format: <schemaName>.<tableName>.<columnName>.
 To match the name of a column, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the column; the expression does not match substrings that might be present in a column name.
 You can specify multiple properties with different lengths in a single configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>converters</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]string</td>
        <td>
          Enumerates a comma-separated list of the symbolic names of the [custom converter](https://debezium.io/documentation/reference/stable/development/converters.html#custom-converters) instances that the connector can use. For example,

```
isbn:
  type: io.debezium.test.IsbnConverter
  schemaName: io.debezium.postgresql.type.Isbn
```

You must set the converters property to enable the connector to use a custom converter.
 For each converter that you configure for a connector, you must also add a .type property, which specifies the fully-qualified name of the class that implements the converter interface.
If you want to further control the behavior of a configured converter, you can add one or more configuration parameters to pass values to the converter. To associate any additional configuration parameter with a converter, prefix the parameter names with the symbolic name of the converter.
 Each property is converted from myPropertyName to my.property.name
<br/>
        </td>
      </tr>
      <tr>
        <td><b>customMetricTags</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          The custom metric tags will accept key-value pairs to customize the MBean object name which should be appended the end of regular name, each key would represent a tag for the MBean object name, and the corresponding value would be the value of that tag the key is. For example:

```
customMetricTags:
  k1: v1
  k2: v2
```
<br/>
        </td>
      </tr>
      <tr>
        <td><b>databaseInitialStatements</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of SQL statements that the connector executes when it establishes a JDBC connection to the database.
 The connector may establish JDBC connections at its own discretion. Consequently, this property is useful for configuration of session parameters only, and not for executing DML statements.
 The connector does not execute these statements when it creates a connection for reading the transaction log.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>datatypePropagateSourceType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `[.*]`. An optional, list of regular expressions that specify the fully-qualified names of data types that are defined for columns in a database. When this property is set, for columns with matching data types, the connector emits event records that include the following extra fields in their schema:

* `__debezium.source.column.type`
* `__debezium.source.column.length`
* `__debezium.source.column.scale`

These parameters propagate a column’s original type name and length (for variable-width types), respectively.
 Enabling the connector to emit this extra data can assist in properly sizing specific numeric or character-based columns in sink databases.
 The fully-qualified name of a column observes one of the following formats: databaseName.tableName.typeName, or databaseName.schemaName.tableName.typeName.
 To match the name of a data type, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the data type; the expression does not match substrings that might be present in a type name.
 For the list of PostgreSQL-specific data type names, see the [PostgreSQL data type mappings](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-data-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>decimalHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `precise`. Specifies how the connector should handle values for DECIMAL and NUMERIC columns:

* `precise`: represents values by using java.math.BigDecimal to represent values in binary form in change events.
* `double`: represents values by using double values, which might result in a loss of precision but which is easier to use.
* `string`: encodes values as formatted strings, which are easy to consume but semantic information about the real type is lost. For more information, see [Decimal types](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-decimal-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>errorsMaxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `-1`. Specifies how the connector responds after an operation that results in a retriable error, such as a connection error.

Set one of the following options:

* `-1`: No limit. The connector always restarts automatically, and retries the operation, regardless of the number of previous failures.
* `0`: Disabled. The connector fails immediately, and never retries the operation. User intervention is required to restart the connector.
* `> 0`: The connector restarts automatically until it reaches the specified maximum number of retries. After the next failure, the connector stops, and user intervention is required to restart it.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>eventProcessingFailureHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `fail`. Specifies how the connector should react to exceptions during processing of events:

* `fail`: propagates the exception, indicates the offset of the problematic event, and causes the connector to stop.
* `warn`: logs the offset of the problematic event, skips that event, and continues processing.
* `skip`: skips the problematic event and continues processing.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>fieldNameAdjustmentMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `none`. Specifies how field names should be adjusted for compatibility with the message converter used by the connector. Possible settings:

* `none` does not apply any adjustment.
* `avro` replaces the characters that cannot be used in the Avro type name with underscore.
* `avro_unicode` replaces the underscore or characters that cannot be used in the Avro type name with corresponding unicode like _uxxxx. Note: _ is an escape sequence like backslash in Java

For more information, see [Avro naming](https://debezium.io/documentation/reference/stable/configuration/avro.html#avro-naming).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>flushLsnSource</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Determines whether the connector should commit the LSN of the processed records in the source postgres database so that the WAL logs can be deleted. Specify false if you don’t want the connector to do this. Please note that if set to false LSN will not be acknowledged by Debezium and as a result WAL logs will not be cleared which might result in disk space issues. User is expected to handle the acknowledgement of LSN outside Debezium.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>heartbeatActionQuery</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Specifies a query that the connector executes on the source database when the connector sends a heartbeat message.
 This is useful for resolving the situation described in [WAL disk space consumption](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-wal-disk-space), where capturing changes from a low-traffic database on the same host as a high-traffic database prevents Debezium from processing WAL records and thus acknowledging WAL positions with the database. To address this situation, create a heartbeat table in the low-traffic database, and set this property to a statement that inserts records into that table, for example:

 ```
 INSERT INTO test_heartbeat_table (text) VALUES ('test_heartbeat')
 ```
 
 This allows the connector to receive changes from the low-traffic database and acknowledge their LSNs, which prevents unbounded WAL growth on the database host.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>heartbeatIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `0`. Controls how frequently the connector sends heartbeat messages to a Kafka topic. The default behavior is that the connector does not send heartbeat messages.
 Heartbeat messages are useful for monitoring whether the connector is receiving change events from the database. Heartbeat messages might help decrease the number of change events that need to be re-sent when a connector restarts. To send heartbeat messages, set this property to a positive integer, which indicates the number of milliseconds between heartbeat messages.
 Heartbeat messages are needed when there are many updates in a database that is being tracked but only a tiny number of updates are related to the table(s) and schema(s) for which the connector is capturing changes. In this situation, the connector reads from the database transaction log as usual but rarely emits change records to Kafka. This means that no offset updates are committed to Kafka and the connector does not have an opportunity to send the latest retrieved LSN to the database. The database retains WAL files that contain events that have already been processed by the connector. Sending heartbeat messages enables the connector to send the latest retrieved LSN to the database, which allows the database to reclaim disk space being used by no longer needed WAL files.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hstoreHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `json`. Specifies how the connector should handle values for hstore columns:

* `map`: represents values by using MAP.
* `json`: represents values by using json string. This setting encodes values as formatted strings such as {"key" : "val"}. For more information, see [PostgreSQL HSTORE type](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-hstore-type).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>includeUnknownDatatypes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Specifies connector behavior when the connector encounters a field whose data type is unknown. The default behavior is that the connector omits the field from the change event and logs a warning.
 Set this property to true if you want the change event to contain an opaque binary representation of the field. This lets consumers decode the field. You can control the exact representation by setting the binaryHandlingMode property.
> *NOTE*: Consumers risk backward compatibility issues when `includeUnknownDatatypes` is set to `true`. Not only may the database-specific binary representation change between releases, but if the data type is eventually supported by Debezium, the data type will be sent downstream in a logical type, which would require adjustments by consumers. In general, when encountering unsupported data types, create a feature request so that support can be added.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>incrementalSnapshotChunkSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `1024`. The maximum number of rows that the connector fetches and reads into memory during an incremental snapshot chunk. Increasing the chunk size provides greater efficiency, because the snapshot runs fewer snapshot queries of a greater size. However, larger chunk sizes also require more memory to buffer the snapshot data. Adjust the chunk size to a value that provides the best performance in your environment.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>incrementalSnapshotWatermarkingStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `insert_insert`. Specifies the watermarking mechanism that the connector uses during an incremental snapshot to deduplicate events that might be captured by an incremental snapshot and then recaptured after streaming resumes.

You can specify one of the following options:

* `insert_insert`: When you send a signal to initiate an incremental snapshot, for every chunk that Debezium reads during the snapshot, it writes an entry to the signaling data collection to record the signal to open the snapshot window. After the snapshot completes, Debezium inserts a second entry to record the closing of the window.
* `insert_delete`: When you send a signal to initiate an incremental snapshot, for every chunk that Debezium reads, it writes a single entry to the signaling data collection to record the signal to open the snapshot window. After the snapshot completes, this entry is removed. No entry is created for the signal to close the snapshot window. Set this option to prevent rapid growth of the signaling data collection.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>intervalHandlingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `numeric`. Specifies how the connector should handle values for interval columns:

 * `numeric`: represents intervals using approximate number of microseconds.
 * `string`: represents intervals exactly by using the string pattern representation P<years>Y<months>M<days>DT<hours>H<minutes>M<seconds>S. For example: P1Y2M3DT4H5M6.78S. For more information, see [PostgreSQL basic types](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-basic-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxBatchSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `2048`. Positive integer value that specifies the maximum size of each batch of events that the connector processes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxQueueSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `8192`. Positive integer value that specifies the maximum number of records that the blocking queue can hold. When Debezium reads events streamed from the database, it places the events in the blocking queue before it writes them to Kafka. The blocking queue can provide backpressure for reading change events from the database in cases where the connector ingests messages faster than it can write them to Kafka, or when Kafka becomes unavailable. Events that are held in the queue are disregarded when the connector periodically records offsets. Always set the value of maxQueueSize to be larger than the value of maxBatchSize.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxQueueSizeInBytes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `0`. A long integer value that specifies the maximum volume of the blocking queue in bytes. By default, volume limits are not specified for the blocking queue. To specify the number of bytes that the queue can consume, set this property to a positive long value.
 If maxQueueSize is also set, writing to the queue is blocked when the size of the queue reaches the limit specified by either property. For example, if you set maxQueueSize=1000, and maxQueueSizeInBytes=5000, writing to the queue is blocked after the queue contains 1000 records, or after the volume of the records in the queue reaches 5000 bytes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>messageKeyColumns</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          A list of expressions that specify the columns that the connector uses to form custom message keys for change event records that it publishes to the Kafka topics for specified tables.
 By default, Debezium uses the primary key column of a table as the message key for records that it emits. In place of the default, or to specify a key for tables that lack a primary key, you can configure custom message keys based on one or more columns.
 To establish a custom message key for a table, list the table, followed by the columns to use as the message key. Each list entry takes the following format:
 <fully-qualified_tableName>:<keyColumn>,<keyColumn>
 To base a table key on multiple column names, insert commas between the column names.
 Each fully-qualified table name is a regular expression in the following format:
 <schemaName>.<tableName>
 The property can include entries for multiple tables. Use a semicolon to separate table entries in the list.
 The following example sets the message key for the tables inventory.customers and purchase.orders:
 inventory.customers:pk1,pk2;(.*).purchaseorders:pk3,pk4
 For the table inventory.customer, the columns pk1 and pk2 are specified as the message key. For the purchaseorders tables in any schema, the columns pk3 and pk4 server as the message key.
 There is no limit to the number of columns that you use to create custom message keys. However, it’s best to use the minimum number that are required to specify a unique key.
 Note that having this property set and REPLICA IDENTITY set to DEFAULT on the tables, will cause the tombstone events to not be created properly if the key columns are not part of the primary key of the table. Setting REPLICA IDENTITY to FULL is the only solution.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>moneyFractionDigits</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `2`. Specifies how many decimal digits should be used when converting Postgres money type to java.math.BigDecimal, which represents the values in change events. Applicable only when decimalHandlingMode is set to precise.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>notificationEnabledChannels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          List of notification channel names that are enabled for the connector. By default, the following channels are available: sink, log and jmx. Optionally, you can also implement a [custom notification channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-enabling-custom-signaling-channel).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pluginName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `pgoutput`. The name of the [PostgreSQL logical decoding plug-in](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-output-plugin) installed on the PostgreSQL server. Supported values are decoderbufs, and pgoutput.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pollIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `500`. Positive integer value that specifies the number of milliseconds the connector should wait for new change events to appear before it starts processing a batch of events. Defaults to 500 milliseconds.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>provideTransactionMetadata</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. Determines whether the connector generates events with transaction boundaries and enriches change event envelopes with transaction metadata. Specify true if you want the connector to do this. For more information, see [Transaction metadata](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-transaction-metadata).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>publicationAutocreateMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `all_tables`. Applies only when streaming changes by using [the pgoutput plug-in](https://www.postgresql.org/docs/current/sql-createpublication.html). The setting determines how creation of a [publication](https://www.postgresql.org/docs/current/logical-replication-publication.html) should work. Specify one of the following values:

* `all_tables` - If a publication exists, the connector uses it. If a publication does not exist, the connector creates a publication for all tables in the database for which the connector is capturing changes. For the connector to create a publication it must access the database through a database user account that has permission to create publications and perform replications. You grant the required permission by using the following SQL command CREATE PUBLICATION <publication_name> FOR ALL TABLES;.
* `disabled` - The connector does not attempt to create a publication. A database administrator or the user configured to perform replications must have created the publication before running the connector. If the connector cannot find the publication, the connector throws an exception and stops.
* `filtered` - If a publication exists, the connector uses it. If no publication exists, the connector creates a new publication for tables that match the current filter configuration as specified by the schema.include.list, schema.exclude.list, and table.include.list, and table.exclude.list connector configuration properties. For example: CREATE PUBLICATION <publication_name> FOR TABLE <tbl1, tbl2, tbl3>. If the publication exists, the connector updates the publication for tables that match the current filter configuration. For example: ALTER PUBLICATION <publication_name> SET TABLE <tbl1, tbl2, tbl3>.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>publicationName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default <SGStream name>.<SGStream namespace> (with all characters that are not `[a-zA-Z0-9]` changed to `_` character). The name of the PostgreSQL publication created for streaming changes when using pgoutput. This publication is created at start-up if it does not already exist and it includes all tables. Debezium then applies its own include/exclude list filtering, if configured, to limit the publication to change events for the specific tables of interest. The connector user must have superuser permissions to create this publication, so it is usually preferable to create the publication before starting the connector for the first time. If the publication already exists, either for all tables or configured with a subset of tables, Debezium uses the publication as it is defined.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>replicaIdentityAutosetValues</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The setting determines the value for [replica identity](https://www.postgresql.org/docs/current/sql-altertable.html#SQL-ALTERTABLE-REPLICA-IDENTITY) at table level.
  This option will overwrite the existing value in database. A comma-separated list of regular expressions that match fully-qualified tables and replica identity value to be used in the table.
  Each expression must match the pattern '<fully-qualified table name>:<replica identity>', where the table name could be defined as (SCHEMA_NAME.TABLE_NAME), and the replica identity values are:
  DEFAULT - Records the old values of the columns of the primary key, if any. This is the default for non-system tables.
  INDEX index_name - Records the old values of the columns covered by the named index, that must be unique, not partial, not deferrable, and include only columns marked NOT NULL. If this index is dropped, the behavior is the same as NOTHING.
  FULL - Records the old values of all columns in the row.
  NOTHING - Records no information about the old row. This is the default for system tables.
  For example,
  schema1.*:FULL,schema2.table2:NOTHING,schema2.table3:INDEX idx_name
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retriableRestartConnectorWaitMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000` (10 seconds). The number of milliseconds to wait before restarting a connector after a retriable error occurs.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>schemaNameAdjustmentMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `none`. Specifies how schema names should be adjusted for compatibility with the message converter used by the connector. Possible settings:

* `none` does not apply any adjustment.
* `avro` replaces the characters that cannot be used in the Avro type name with underscore.
* `avro_unicode` replaces the underscore or characters that cannot be used in the Avro type name with corresponding unicode like _uxxxx. Note: _ is an escape sequence like backslash in Java
<br/>
        </td>
      </tr>
      <tr>
        <td><b>schemaRefreshMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `columns_diff`. Specify the conditions that trigger a refresh of the in-memory schema for a table.

* `columns_diff`: is the safest mode. It ensures that the in-memory schema stays in sync with the database table’s schema at all times.
* `columns_diff_exclude_unchanged_toast`: instructs the connector to refresh the in-memory schema cache if there is a discrepancy with the schema derived from the incoming message, unless unchanged TOASTable data fully accounts for the discrepancy.

This setting can significantly improve connector performance if there are frequently-updated tables that have TOASTed data that are rarely part of updates. However, it is possible for the in-memory schema to become outdated if TOASTable columns are dropped from the table.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>signalDataCollection</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Fully-qualified name of the data collection that is used to send signals to the connector. Use the following format to specify the collection name: <schemaName>.<tableName>
<br/>
        </td>
      </tr>
      <tr>
        <td><b>signalEnabledChannels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `[sgstream-annotations]`. List of the signaling channel names that are enabled for the connector. By default, the following channels are available: sgstream-annotations, source, kafka, file and jmx. Optionally, you can also implement a [custom signaling channel](https://debezium.io/documentation/reference/stable/configuration/signalling.html#debezium-signaling-enabling-custom-signaling-channel).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipMessagesWithoutChange</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. Specifies whether to skip publishing messages when there is no change in included columns. This would essentially filter messages if there is no change in columns included as per includes or excludes fields. Note: Only works when REPLICA IDENTITY of the table is set to FULL
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skippedOperations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default `none`. A list of operation types that will be skipped during streaming. The operations include: c for inserts/create, u for updates, d for deletes, t for truncates, and none to not skip any operations. By default, no operations are skipped.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotDropOnStop</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Whether or not to delete the logical replication slot when the connector stops in a graceful, expected way. The default behavior is that the replication slot remains configured for the connector when the connector stops. When the connector restarts, having the same replication slot enables the connector to start processing where it left off. Set to true in only testing or development environments. Dropping the slot allows the database to discard WAL segments. When the connector restarts it performs a new snapshot or it can continue from a persistent offset in the Kafka Connect offsets topic.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotMaxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `6`. If connecting to a replication slot fails, this is the maximum number of consecutive attempts to connect.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default <SGStream namespace>.<SGStream name> (with all characters that are not `[a-zA-Z0-9]` changed to `_` character). The name of the PostgreSQL logical decoding slot that was created for streaming changes from a particular plug-in for a particular database/schema. The server uses this slot to stream events to the Debezium connector that you are configuring.

Slot names must conform to [PostgreSQL replication slot naming rules](https://www.postgresql.org/docs/current/static/warm-standby.html#STREAMING-REPLICATION-SLOTS-MANIPULATION), which state: "Each replication slot has a name, which can contain lower-case letters, numbers, and the underscore character."
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotRetryDelayMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000` (10 seconds). The number of milliseconds to wait between retry attempts when the connector fails to connect to a replication slot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>slotStreamParams</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Parameters to pass to the configured logical decoding plug-in. For example:

```
slotStreamParams:
  add-tables: "public.table,public.table2"
  include-lsn: "true"
```
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotDelayMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          An interval in milliseconds that the connector should wait before performing a snapshot when the connector starts. If you are starting multiple connectors in a cluster, this property is useful for avoiding snapshot interruptions, which might cause re-balancing of connectors.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotFetchSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10240`. During a snapshot, the connector reads table content in batches of rows. This property specifies the maximum number of rows in a batch.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotIncludeCollectionList</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Default <All tables / All tables filtered in `includes` field / All tables that are not filtered out in `excludes` field>. An optional, list of regular expressions that match the fully-qualified names (<schemaName>.<tableName>) of the tables to include in a snapshot. The specified items must be named in the connector’s table.include.list property. This property takes effect only if the connector’s snapshotMode property is set to a value other than `never`. This property does not affect the behavior of incremental snapshots.
   To match the name of a table, Debezium applies the regular expression that you specify as an anchored regular expression. That is, the specified expression is matched against the entire name string of the table; it does not match substrings that might be present in a table name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotLockTimeoutMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. Positive integer value that specifies the maximum amount of time (in milliseconds) to wait to obtain table locks when performing a snapshot. If the connector cannot acquire table locks in this time interval, the snapshot fails. [How the connector performs snapshots](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-snapshots) provides details.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotLockingMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `none`. Specifies how the connector holds locks on tables while performing a schema snapshot. Set one of the following options:

* `shared`: The connector holds a table lock that prevents exclusive table access during the initial portion phase of the snapshot in which database schemas and other metadata are read. After the initial phase, the snapshot no longer requires table locks.
* `none`: The connector avoids locks entirely. Do not use this mode if schema changes might occur during the snapshot.

> *WARNING*: Do not use this mode if schema changes might occur during the snapshot.

* `custom`: The connector performs a snapshot according to the implementation specified by the snapshotLockingModeCustomName property, which is a custom implementation of the io.debezium.spi.snapshot.SnapshotLock interface.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotLockingModeCustomName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When snapshotLockingMode is set to custom, use this setting to specify the name of the custom implementation provided in the name() method that is defined by the 'io.debezium.spi.snapshot.SnapshotLock' interface. For more information, see [custom snapshotter SPI](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#connector-custom-snapshot).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotMaxThreads</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `1`. Specifies the number of threads that the connector uses when performing an initial snapshot. To enable parallel initial snapshots, set the property to a value greater than 1. In a parallel initial snapshot, the connector processes multiple tables concurrently. This feature is incubating.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `initial`. Specifies the criteria for performing a snapshot when the connector starts:

* `always` - The connector performs a snapshot every time that it starts. The snapshot includes the structure and data of the captured tables. Specify this value to populate topics with a complete representation of the data from the captured tables every time that the connector starts. After the snapshot completes, the connector begins to stream event records for subsequent database changes.
* `initial` - The connector performs a snapshot only when no offsets have been recorded for the logical server name.
* `initial_only` - The connector performs an initial snapshot and then stops, without processing any subsequent changes.
* `no_data` - The connector never performs snapshots. When a connector is configured this way, after it starts, it behaves as follows: If there is a previously stored LSN in the Kafka offsets topic, the connector continues streaming changes from that position. If no LSN is stored, the connector starts streaming changes from the point in time when the PostgreSQL logical replication slot was created on the server. Use this snapshot mode only when you know all data of interest is still reflected in the WAL.
* `never` - Deprecated see no_data.
* `when_needed` - After the connector starts, it performs a snapshot only if it detects one of the following circumstances: 
  It cannot detect any topic offsets.
  A previously recorded offset specifies a log position that is not available on the server.
* `configuration_based` - With this option, you control snapshot behavior through a set of connector properties that have the prefix 'snapshotModeConfigurationBased'.
* `custom` - The connector performs a snapshot according to the implementation specified by the snapshotModeCustomName property, which defines a custom implementation of the io.debezium.spi.snapshot.Snapshotter interface.

For more information, see the [table of snapshot.mode options](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-connector-snapshot-mode-options).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotData</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector includes table data when it performs a snapshot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotOnDataError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, this property specifies whether the connector attempts to snapshot table data if it does not find the last committed offset in the transaction log. Set the value to true to instruct the connector to perform a new snapshot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotOnSchemaError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector includes table schema in a snapshot if the schema history topic is not available.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedSnapshotSchema</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector includes the table schema when it performs a snapshot.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeConfigurationBasedStartStream</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. If the snapshotMode is set to configuration_based, set this property to specify whether the connector begins to stream change events after a snapshot completes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotModeCustomName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When snapshotMode is set as custom, use this setting to specify the name of the custom implementation provided in the name() method that is defined by the 'io.debezium.spi.snapshot.Snapshotter' interface. The provided implementation is called after a connector restart to determine whether to perform a snapshot. For more information, see [custom snapshotter SPI](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#connector-custom-snapshot).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotQueryMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `select_all`. Specifies how the connector queries data while performing a snapshot. Set one of the following options:

* `select_all`: The connector performs a select all query by default, optionally adjusting the columns selected based on the column include and exclude list configurations.
* `custom`: The connector performs a snapshot query according to the implementation specified by the snapshotQueryModeCustomName property, which defines a custom implementation of the io.debezium.spi.snapshot.SnapshotQuery interface. This setting enables you to manage snapshot content in a more flexible manner compared to using the snapshotSelectStatementOverrides property.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotQueryModeCustomName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When snapshotQueryMode is set as custom, use this setting to specify the name of the custom implementation provided in the name() method that is defined by the 'io.debezium.spi.snapshot.SnapshotQuery' interface. For more information, see [custom snapshotter SPI](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#connector-custom-snapshot).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotSelectStatementOverrides</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Specifies the table rows to include in a snapshot. Use the property if you want a snapshot to include only a subset of the rows in a table. This property affects snapshots only. It does not apply to events that the connector reads from the log.
 The property contains a hierarchy of fully-qualified table names in the form <schemaName>.<tableName>. For example,

```
snapshotSelectStatementOverrides: 
  "customers.orders": "SELECT * FROM [customers].[orders] WHERE delete_flag = 0 ORDER BY id DESC"
```

In the resulting snapshot, the connector includes only the records for which delete_flag = 0.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>statusUpdateIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. Frequency for sending replication connection status updates to the server, given in milliseconds. The property also controls how frequently the database status is checked to detect a dead connection in case the database was shut down.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timePrecisionMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `adaptive`. Time, date, and timestamps can be represented with different kinds of precision:

* `adaptive`: captures the time and timestamp values exactly as in the database using either millisecond, microsecond, or nanosecond precision values based on the database column’s type.
* `adaptive_time_microseconds`: captures the date, datetime and timestamp values exactly as in the database using either millisecond, microsecond, or nanosecond precision values based on the database column’s type. An exception is TIME type fields, which are always captured as microseconds.
* `connect`: always represents time and timestamp values by using Kafka Connect’s built-in representations for Time, Date, and Timestamp, which use millisecond precision regardless of the database columns' precision. For more information, see [temporal values](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-temporal-types).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tombstonesOnDelete</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Controls whether a delete event is followed by a tombstone event.

* `true` - a delete operation is represented by a delete event and a subsequent tombstone event.
* `false` - only a delete event is emitted.

After a source record is deleted, emitting a tombstone event (the default behavior) allows Kafka to completely delete all events that pertain to the key of the deleted row in case [log compaction](https://kafka.apache.org/documentation/#compaction) is enabled for the topic.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicCacheSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. The size used for holding the topic names in bounded concurrent hash map. This cache will help to determine the topic name corresponding to a given data collection.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicDelimiter</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `.`. Specify the delimiter for topic name, defaults to ".".
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicHeartbeatPrefix</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `__debezium-heartbeat`. Controls the name of the topic to which the connector sends heartbeat messages. For example, if the topic prefix is fulfillment, the default topic name is __debezium-heartbeat.fulfillment.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicNamingStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `io.debezium.schema.SchemaTopicNamingStrategy`. The name of the TopicNamingStrategy class that should be used to determine the topic name for data change, schema change, transaction, heartbeat event etc., defaults to SchemaTopicNamingStrategy.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topicTransaction</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `transaction`. Controls the name of the topic to which the connector sends transaction metadata messages. For example, if the topic prefix is fulfillment, the default topic name is fulfillment.transaction.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unavailableValuePlaceholder</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `__debezium_unavailable_value`. Specifies the constant that the connector provides to indicate that the original value is a toasted value that is not provided by the database. If the setting of unavailable.value.placeholder starts with the hex: prefix it is expected that the rest of the string represents hexadecimally encoded octets. For more information, see [toasted values](https://debezium.io/documentation/reference/stable/connectors/postgresql.html#postgresql-toasted-values).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>xminFetchIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `0`. How often, in milliseconds, the XMIN will be read from the replication slot. The XMIN value provides the lower bounds of where a new replication slot could start from. The default value of 0 disables tracking XMIN tracking.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.source.sgCluster.password
<sup><sup>[↩ Parent](#sgstreamspecsourcesgcluster)</sup></sup>

The password used by the CDC process to connect to the database.

If not specified the default superuser password will be used.


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
          The Secret key where the password is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the password is stored.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.source.sgCluster.username
<sup><sup>[↩ Parent](#sgstreamspecsourcesgcluster)</sup></sup>

The username used by the CDC process to connect to the database.

If not specified the default superuser username (by default postgres) will be used.


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
          The Secret key where the username is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the username is stored.
<br/>
        </td>
      </tr></tbody>
</table>

### SGStream.spec.target
<sup><sup>[↩ Parent](#sgstreamspec)</sup></sup>

The target of this sream.


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
        <td><b>type</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Indicate the type of target of this stream. Possible values are:

* `CloudEvent`: events will be sent to a cloud event receiver.
* `PgLambda`: events will trigger the execution of a lambda script by integrating with [Knative Service](https://knative.dev/docs/serving/) (Knative must be already installed).
* `SGCluster`: events will be sinked to an SGCluster allowing migration of data.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetcloudevent">cloudEvent</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration section for `CloudEvent` target type.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetpglambda">pgLambda</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration section for `PgLambda` target type.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetsgcluster">sgCluster</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The configuration of the data target required when type is `SGCluster`.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.target.cloudEvent
<sup><sup>[↩ Parent](#sgstreamspectarget)</sup></sup>

Configuration section for `CloudEvent` target type.


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
        <td><b>binding</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The CloudEvent binding (http by default).

Only http is supported at the moment.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>format</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The CloudEvent format (json by default).

Only json is supported at the moment.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetcloudeventhttp">http</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The http binding configuration.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.cloudEvent.http
<sup><sup>[↩ Parent](#sgstreamspectargetcloudevent)</sup></sup>

The http binding configuration.

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
        <td><b>url</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The URL used to send the CloudEvents to the endpoint.<br/>
        </td>
      </tr>
      <tr>
        <td><b>connectTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the connect timeout.

Value 0 represents infinity (default). Negative values are not allowed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>headers</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Headers to include when sending CloudEvents to the endpoint.<br/>
        </td>
      </tr>
      <tr>
        <td><b>readTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the read timeout. The value is the timeout to read a response.

Value 0 represents infinity (default). Negative values are not allowed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retryBackoffDelay</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum amount of delay in seconds after an error before retrying again.

The initial delay will use 10% of this value and then increase the value exponentially up to the maximum amount of seconds specified with this field.
<br/>
          <br/>
            <i>Default</i>: 60<br/>
        </td>
      </tr>
      <tr>
        <td><b>retryLimit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Set the retry limit. When set the event will be sent again after an error for the specified limit of times. When not set the event will be sent again after an error.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipHostnameVerification</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` disable hostname verification.<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.target.pgLambda
<sup><sup>[↩ Parent](#sgstreamspectarget)</sup></sup>

Configuration section for `PgLambda` target type.


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
        <td><b><a href="#sgstreamspectargetpglambdaknative">knative</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Knative Service configuration.<br/>
        </td>
      </tr>
      <tr>
        <td><b>script</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Script to execute. This field is mutually exclusive with `scriptFrom` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetpglambdascriptfrom">scriptFrom</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>scriptType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The PgLambda script format (javascript by default).

* `javascript`: the script will receive the following variable:
  * `request`: the HTTP request object. See https://nodejs.org/docs/latest-v18.x/api/http.html#class-httpclientrequest
  * `response`: the HTTP response object. See https://nodejs.org/docs/latest-v18.x/api/http.html#class-httpserverresponse
  * `event`: the CloudEvent event object. See https://github.com/cloudevents/sdk-javascript
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.pgLambda.knative
<sup><sup>[↩ Parent](#sgstreamspectargetpglambda)</sup></sup>

Knative Service configuration.

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
        <td><b>annotations</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Annotations to set to Knative Service<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetpglambdaknativehttp">http</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          PgLambda uses a CloudEvent http binding to send events to the Knative Service. This section allow to modify the configuration of this binding.<br/>
        </td>
      </tr>
      <tr>
        <td><b>labels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Labels to set to Knative Service<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.pgLambda.knative.http
<sup><sup>[↩ Parent](#sgstreamspectargetpglambdaknative)</sup></sup>

PgLambda uses a CloudEvent http binding to send events to the Knative Service. This section allow to modify the configuration of this binding.

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
        <td><b>connectTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the connect timeout.

Value 0 represents infinity (default). Negative values are not allowed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>headers</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          Headers to include when sending CloudEvents to the endpoint.<br/>
        </td>
      </tr>
      <tr>
        <td><b>readTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Set the read timeout. The value is the timeout to read a response.

Value 0 represents infinity (default). Negative values are not allowed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retryBackoffDelay</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum amount of delay in seconds after an error before retrying again.

The initial delay will use 10% of this value and then increase the value exponentially up to the maximum amount of seconds specified with this field.
<br/>
          <br/>
            <i>Default</i>: 60<br/>
        </td>
      </tr>
      <tr>
        <td><b>retryLimit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Set the retry limit. When set the event will be sent again after an error for the specified limit of times. When not set the event will be sent again after an error.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipHostnameVerification</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` disable hostname verification.<br/>
        </td>
      </tr>
      <tr>
        <td><b>url</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The URL used to send the CloudEvents to the endpoint.<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.pgLambda.scriptFrom
<sup><sup>[↩ Parent](#sgstreamspectargetpglambda)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


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
        <td><b><a href="#sgstreamspectargetpglambdascriptfromconfigmapkeyref">configMapKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the script to execute. This field is mutually exclusive with `secretKeyRef` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetpglambdascriptfromsecretkeyref">secretKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the script to execute. This field is mutually exclusive with `configMapKeyRef` field.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.pgLambda.scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgstreamspectargetpglambdascriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the script to execute. This field is mutually exclusive with `secretKeyRef` field.


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
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key name within the ConfigMap that contains the script to execute.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the ConfigMap that contains the script to execute.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.pgLambda.scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgstreamspectargetpglambdascriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.32/#secretkeyselector-v1-core) that contains the script to execute. This field is mutually exclusive with `configMapKeyRef` field.


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
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

#### SGStream.spec.target.sgCluster
<sup><sup>[↩ Parent](#sgstreamspectarget)</sup></sup>

The configuration of the data target required when type is `SGCluster`.


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
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target SGCluster name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target database name to which the data will be migrated to.

If not specified the default postgres database will be targeted.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>ddlImportRoleSkipFilter</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Allow to set a [SIMILAR TO regular expression](https://www.postgresql.org/docs/current/functions-matching.html#FUNCTIONS-SIMILARTO-REGEXP) to match the names of the roles to skip during import of DDL.

When not set and source is an SGCluster will match the superuser, replicator and authenticator usernames.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetsgclusterdebeziumproperties">debeziumProperties</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Specific property of the debezium JDBC sink.

See https://debezium.io/documentation/reference/stable/connectors/jdbc.html#jdbc-connector-configuration

Each property is converted from myPropertyName to my.property.name
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetsgclusterpassword">password</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The password used by the CDC sink process to connect to the database.

If not specified the default superuser password will be used.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipDdlImport</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` disable import of DDL and tables will be created on demand by Debezium.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipDropIndexesAndConstraints</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` disable drop of indexes and constraints that improve snapshotting performance.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>skipRestoreIndexesAfterSnapshot</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` disable restore of indexes on the first non-snapshot event. This option is required when using incremental snapshotting. This option is ignored when `skipDropIndexesAndConstraints` is set to `true`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamspectargetsgclusterusername">username</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The username used by the CDC sink process to connect to the database.

If not specified the default superuser username (by default postgres) will be used.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.sgCluster.debeziumProperties
<sup><sup>[↩ Parent](#sgstreamspectargetsgcluster)</sup></sup>

Specific property of the debezium JDBC sink.

See https://debezium.io/documentation/reference/stable/connectors/jdbc.html#jdbc-connector-configuration

Each property is converted from myPropertyName to my.property.name


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
        <td><b>batchSize</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `500`. Specifies how many records to attempt to batch together into the destination table.
> Note that if you set `consumerMaxPollRecords` in the Connect worker properties to a value lower than `batchSize`, batch processing will be caped by `consumerMaxPollRecords` and the desired `batchSize` won’t be reached. You can also configure the connector’s underlying consumer’s `maxPollRecords` using `consumerOverrideMaxPollRecords` in the connector configuration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>columnNamingStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `io.debezium.connector.jdbc.naming.DefaultColumnNamingStrategy`. Specifies the fully-qualified class name of a ColumnNamingStrategy implementation that the connector uses to resolve column names from event field names.
By default, the connector uses the field name as the column name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>connectionPoolAcquire_increment</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `32`. Specifies the number of connections that the connector attempts to acquire if the connection pool exceeds its maximum size.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>connectionPoolMax_size</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `32`. Specifies the maximum number of concurrent connections that the pool maintains.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>connectionPoolMin_size</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `5`. Specifies the minimum number of connections in the pool.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>connectionPoolTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `1800`. Specifies the number of seconds that an unused connection is kept before it is discarded.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>databaseTime_zone</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `UTC`. Specifies the timezone used when inserting JDBC temporal values.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>deleteEnabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Specifies whether the connector processes DELETE or tombstone events and removes the corresponding row from the database. Use of this option requires that you set the `primaryKeyMode` to `record_key`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>dialectPostgresPostgisSchema</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `public`. Specifies the schema name where the PostgreSQL PostGIS extension is installed. The default is `public`; however, if the PostGIS extension was installed in another schema, this property should be used to specify the alternate schema name.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>dialectSqlserverIdentityInsert</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. Specifies whether the connector automatically sets an IDENTITY_INSERT before an INSERT or UPSERT operation into the identity column of SQL Server tables, and then unsets it immediately after the operation. When the default setting (`false`) is in effect, an INSERT or UPSERT operation into the IDENTITY column of a table results in a SQL exception.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>insertMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `upsert`. Specifies the strategy used to insert events into the database. The following options are available:
* `insert`: Specifies that all events should construct INSERT-based SQL statements. Use this option only when no primary key is used, or when you can be certain that no updates can occur to rows with existing primary key values.
* `update`: Specifies that all events should construct UPDATE-based SQL statements. Use this option only when you can be certain that the connector receives only events that apply to existing rows.
* `upsert`: Specifies that the connector adds events to the table using upsert semantics. That is, if the primary key does not exist, the connector performs an INSERT operation, and if the key does exist, the connector performs an UPDATE operation. When idempotent writes are required, the connector should be configured to use this option.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryKeyFields</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          Either the name of the primary key column or a comma-separated list of fields to derive the primary key from.
When `primaryKeyMode` is set to `record_key` and the event’s key is a primitive type, it is expected that this property specifies the column name to be used for the key.
When the `primaryKeyMode` is set to `record_key` with a non-primitive key, or record_value, it is expected that this property specifies a comma-separated list of field names from either the key or value. If the primary.key.mode is set to record_key with a non-primitive key, or record_value, and this property is not specifies, the connector derives the primary key from all fields of either the record key or record value, depending on the specified mode.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryKeyMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `record_key`. Specifies how the connector resolves the primary key columns from the event.
* `none`: Specifies that no primary key columns are created.
* `record_key`: Specifies that the primary key columns are sourced from the event’s record key. If the record key is a primitive type, the `primaryKeyFields` property is required to specify the name of the primary key column. If the record key is a struct type, the `primaryKeyFields` property is optional, and can be used to specify a subset of columns from the event’s key as the table’s primary key.
* `record_value`: Specifies that the primary key columns is sourced from the event’s value. You can set the `primaryKeyFields` property to define the primary key as a subset of fields from the event’s value; otherwise all fields are used by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>quoteIdentifiers</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Specifies whether generated SQL statements use quotation marks to delimit table and column names. See the Quoting and case sensitivity section for more details.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>schemaEvolution</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `basic`. Specifies how the connector evolves the destination table schemas. For more information, see Schema evolution. The following options are available:
`none`: Specifies that the connector does not evolve the destination schema.
`basic`: Specifies that basic evolution occurs. The connector adds missing columns to the table by comparing the incoming event’s record schema to the database table structure.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tableNameFormat</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `${original}`. Specifies a string that determines how the destination table name is formatted, based on the topic name of the event. The placeholder ${original} is replaced with the schema name and the table name separated by a point character (`.`).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tableNamingStrategy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `io.stackgres.stream.jobs.migration.StreamMigrationTableNamingStrategy`. Specifies the fully-qualified class name of a TableNamingStrategy implementation that the connector uses to resolve table names from incoming event topic names.
The default behavior is to:
* Replace the ${topic} placeholder in the `tableNameFormat` configuration property with the event’s topic.
* Sanitize the table name by replacing dots (`.`) with underscores (`_`).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>truncateEnabled</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `true`. Specifies whether the connector processes TRUNCATE events and truncates the corresponding tables from the database.
Although support for TRUNCATE statements has been available in Db2 since version 9.7, currently, the JDBC connector is unable to process standard TRUNCATE events that the Db2 connector emits.
To ensure that the JDBC connector can process TRUNCATE events received from Db2, perform the truncation by using an alternative to the standard TRUNCATE TABLE statement. For example:

```
ALTER TABLE <table_name> ACTIVATE NOT LOGGED INITIALLY WITH EMPTY TABLE
```

The user account that submits the preceding query requires ALTER privileges on the table to be truncated.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.sgCluster.password
<sup><sup>[↩ Parent](#sgstreamspectargetsgcluster)</sup></sup>

The password used by the CDC sink process to connect to the database.

If not specified the default superuser password will be used.


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
          The Secret key where the password is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the password is stored.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGStream.spec.target.sgCluster.username
<sup><sup>[↩ Parent](#sgstreamspectargetsgcluster)</sup></sup>

The username used by the CDC sink process to connect to the database.

If not specified the default superuser username (by default postgres) will be used.


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
          The Secret key where the username is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the username is stored.
<br/>
        </td>
      </tr></tbody>
</table>

### SGStream.spec.debeziumEngineProperties
<sup><sup>[↩ Parent](#sgstreamspec)</sup></sup>

See https://debezium.io/documentation/reference/stable/development/engine.html#engine-properties
 Each property is converted from myPropertyName to my.property.name


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
        <td><b>errorsMaxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `-1`. The maximum number of retries on connection errors before failing (-1 = no limit, 0 = disabled, > 0 = num of retries).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>errorsRetryDelayInitialMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `300`. Initial delay (in ms) for retries when encountering connection errors. This value will be doubled upon every retry but won’t exceed errorsRetryDelayMaxMs.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>errorsRetryDelayMaxMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `10000`. Max delay (in ms) between retries when encountering conn
<br/>
        </td>
      </tr>
      <tr>
        <td><b>offsetCommitPolicy</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `io.debezium.engine.spi.OffsetCommitPolicy.PeriodicCommitOffsetPolicy`. The name of the Java class of the commit policy. It defines when offsets commit has to be triggered based on the number of events processed and the time elapsed since the last commit. This class must implement the interface OffsetCommitPolicy. The default is a periodic commity policy based upon time intervals.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>offsetFlushIntervalMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `60000`. Interval at which to try committing offsets. The default is 1 minute.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>offsetFlushTimeoutMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `5000`. Maximum number of milliseconds to wait for records to flush and partition offset data to be committed to offset storage before cancelling the process and restoring the offset data to be committed in a future attempt. The default is 5 seconds.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>predicates</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]string</td>
        <td>
          Predicates can be applied to transformations to make the transformations optional.

An example of the configuration is:

```
predicates:
  headerExists: # (1)
   type: "org.apache.kafka.connect.transforms.predicates.HasHeaderKey" # (2)
   name: "header.name" # (3)
transforms:
  filter: # (4)
    type: "io.debezium.embedded.ExampleFilterTransform" # (5)
    predicate: "headerExists" # (6)
    negate: "true" # (7)
```

1. One predicate is defined - headerExists
2. Implementation of the headerExists predicate is org.apache.kafka.connect.transforms.predicates.HasHeaderKey
3. The headerExists predicate has one configuration option - name
4. One transformation is defined - filter
5. Implementation of the filter transformation is io.debezium.embedded.ExampleFilterTransform
6. The filter transformation requires the predicate headerExists
7. The filter transformation expects the value of the predicate to be negated, making the predicate determine if the header does not exist
<br/>
        </td>
      </tr>
      <tr>
        <td><b>recordProcessingOrder</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Default `ORDERED`. 

Determines how the records should be produced.

* `ORDERED`

  Records are processed sequentially; that is, they are produced in the order in which they were obtained from the database.

* `UNORDERED`

  Records are processed non-sequentially; that is, they can be produced in an different order than in the source database.

The non-sequential processing of the `UNORDERED` option results in better throughput, because records are produced immediately after any SMT processing and message serialization is complete, without waiting for other records. This option doesn’t have any effect when the ChangeConsumer method is provided to the engine.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>recordProcessingShutdownTimeoutMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `1000`. Maximum time in milliseconds to wait for processing submitted records after a task shutdown is called.<br/>
        </td>
      </tr>
      <tr>
        <td><b>recordProcessingThreads</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of threads that are available to process change event records. If no value is specified (the default), the engine uses the Java [ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html) to dynamically adjust the number of threads, based on the current workload. Maximum number of threads is number of CPU cores on given machine. If a value is specified, the engine uses the Java [fixed thread pool](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Executors.html) method to create a thread pool with the specified number of threads. To use all available cores on given machine, set the placeholder value, AVAILABLE_CORES.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>recordProcessingWithSerialConsumer</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Default `false`. Specifies whether the default ChangeConsumer should be created from the provided Consumer, resulting in serial Consumer processing. This option has no effect if you specified the ChangeConsumer interface when you used the API to create the engine.<br/>
        </td>
      </tr>
      <tr>
        <td><b>taskManagementTimeoutMs</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Default `180000`. Time, in milliseconds, that the engine waits for a task’s lifecycle management operations (starting and stopping) to complete.<br/>
        </td>
      </tr>
      <tr>
        <td><b>transforms</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]map[string]string</td>
        <td>
          Before the messages are delivered to the handler it is possible to run them through a pipeline of Kafka Connect Simple Message Transforms (SMT). Each SMT can pass the message unchanged, modify it or filter it out. The chain is configured using property transforms. The property contains a list of logical names of the transformations to be applied (the specified keys). Properties transforms.<logical_name>.type then defines the name of the implementation class for each transformation and transforms.<logical_name>.* configuration options that are passed to the transformation.

An example of the configuration is:

```
transforms: # (1)
  router:
    type: "org.apache.kafka.connect.transforms.RegexRouter" # (2)
    regex: "(.*)" # (3)
    replacement: "trf$1" # (3)
  filter:
    type: "io.debezium.embedded.ExampleFilterTransform" # (4)
```

1. Two transformations are defined - filter and router
2. Implementation of the router transformation is org.apache.kafka.connect.transforms.RegexRouter
3. The router transformation has two configurations options -regex and replacement
4. Implementation of the filter transformation is io.debezium.embedded.ExampleFilterTransform
<br/>
        </td>
      </tr></tbody>
</table>

## SGStream.status
<sup><sup>[↩ Parent](#sgstream)</sup></sup>

Status of a StackGres stream.


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
        <td><b><a href="#sgstreamstatusconditionsindex">conditions</a></b></td>
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
        <td><b><a href="#sgstreamstatusevents">events</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Events status<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The failure message<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamstatussnapshot">snapshot</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Snapshot status<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgstreamstatusstreaming">streaming</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Streaming status<br/>
        </td>
      </tr></tbody>
</table>

### SGStream.status.conditions[index]
<sup><sup>[↩ Parent](#sgstreamstatus)</sup></sup>



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

### SGStream.status.events
<sup><sup>[↩ Parent](#sgstreamstatus)</sup></sup>

Events status

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
        <td><b>lastErrorSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The last error seen sending events that this stream has seen since the last start or metrics reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastEventSent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The last event that the stream has sent since the last start or metrics reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastEventWasSent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          It is true if the last event that the stream has tried to send since the last start or metrics reset was sent successfully.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfErrorsSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of errors sending events that this stream has seen since the last start or metrics reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfEventsSent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of events that this stream has sent since the last start or metrics reset.
<br/>
        </td>
      </tr></tbody>
</table>

### SGStream.status.snapshot
<sup><sup>[↩ Parent](#sgstreamstatus)</sup></sup>

Snapshot status

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
        <td><b>capturedTables</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The list of tables that are captured by the connector.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>chunkFrom</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The lower bound of the primary key set defining the current chunk.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>chunkId</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The identifier of the current snapshot chunk.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>chunkTo</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The upper bound of the primary key set defining the current chunk.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>currentQueueSizeInBytes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The current volume, in bytes, of records in the queue.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastEvent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The last snapshot event that the connector has read.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxQueueSizeInBytes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum buffer of the queue in bytes. This metric is available if max.queue.size.in.bytes is set to a positive long value.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>milliSecondsSinceLastEvent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of milliseconds since the connector has read and processed the most recent event.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>numberOfEventsFiltered</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of events that have been filtered by include/exclude list filtering rules configured on the connector.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>queueRemainingCapacity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The free capacity of the queue used to pass events between the snapshotter and the main Kafka Connect loop.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>queueTotalCapacity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The length the queue used to pass events between the snapshotter and the main Kafka Connect loop.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>remainingTableCount</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of tables that the snapshot has yet to copy.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>rowsScanned</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]integer</td>
        <td>
          Map containing the number of rows scanned for each table in the snapshot. Tables are incrementally added to the Map during processing. Updates every 10,000 rows scanned and upon completing a table.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotAborted</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Whether the snapshot was aborted.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotCompleted</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Whether the snapshot completed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotDurationInSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of seconds that the snapshot has taken so far, even if not complete. Includes also time when snapshot was paused.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotPaused</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Whether the snapshot was paused.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotPausedDurationInSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of seconds that the snapshot was paused. If the snapshot was paused several times, the paused time adds up.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>snapshotRunning</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Whether the snapshot was started.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tableFrom</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The lower bound of the primary key set of the currently snapshotted table.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>tableTo</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The upper bound of the primary key set of the currently snapshotted table.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfEventsSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of events that this connector has seen since last started or reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalTableCount</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of tables that are being included in the snapshot.
<br/>
        </td>
      </tr></tbody>
</table>

### SGStream.status.streaming
<sup><sup>[↩ Parent](#sgstreamstatus)</sup></sup>

Streaming status

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
        <td><b>capturedTables</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The list of tables that are captured by the connector.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>connected</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Flag that denotes whether the connector is currently connected to the database server.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>currentQueueSizeInBytes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The current volume, in bytes, of records in the queue.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastEvent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The last streaming event that the connector has read.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastTransactionId</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Transaction identifier of the last processed transaction.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxQueueSizeInBytes</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum buffer of the queue in bytes. This metric is available if max.queue.size.in.bytes is set to a positive long value.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>milliSecondsBehindSource</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of milliseconds between the last change event’s timestamp and the connector processing it. The values will incoporate any differences between the clocks on the machines where the database server and the connector are running.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>milliSecondsSinceLastEvent</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of milliseconds since the connector has read and processed the most recent event.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>numberOfCommittedTransactions</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of processed transactions that were committed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>numberOfEventsFiltered</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of events that have been filtered by include/exclude list filtering rules configured on the connector.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>queueRemainingCapacity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The free capacity of the queue used to pass events between the streamer and the main Kafka Connect loop.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>queueTotalCapacity</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The length the queue used to pass events between the streamer and the main Kafka Connect loop.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourceEventPosition</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          The coordinates of the last received event.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfCreateEventsSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of create events that this connector has seen since the last start or metrics reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfDeleteEventsSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of delete events that this connector has seen since the last start or metrics reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfEventsSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of events that this connector has seen since the last start or metrics reset.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>totalNumberOfUpdateEventsSeen</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The total number of update events that this connector has seen since the last start or metrics reset.
<br/>
        </td>
      </tr></tbody>
</table>
