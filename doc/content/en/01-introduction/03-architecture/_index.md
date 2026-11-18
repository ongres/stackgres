---
title: "Architecture"
weight: 3
url: /intro/architecture
description: Details about StackGres' architecture.
showToc: true
---

## The Cluster

A StackGres cluster is basically a StatefulSet where each pod is a database instance. The
 StatefulSet guarantees that each pod is always bound to its own persistent volume. Therefore, the
 database instance data will be mapped to the state of a Patroni instance inside Kubernetes.

### StackGres Cluster Architecture diagram

{{< svg "SG_StackGres_Architecture.svg" "StackGres-General_Architecture" >}}

### StackGres Pod Architecture diagram

We use a pattern called sidecar where a main application runs in a container and other containers co-located in the same pod
 are providing side functionalities like connection pooling, export of stats, edge proxying,
 logging dispatcher or database utilities.
 
{{< svg "SG_Diagram-Anatomy_of_the_Pod.svg" "Pod Architecture" >}}

> **UDS:** [Unix Domain Socket](https://en.wikipedia.org/wiki/Unix_domain_socket)

