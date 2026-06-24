---
title: Managing backups
weight: 4
url: /cookbook/managing-backups
description: Recipes for taking backups (SGBackup) and configuring where they are stored (SGObjectStorage).
showToc: true
---

The recipes in this section cover the two custom resources involved in StackGres backups:

- [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}}) — represents a single backup.
  Create one to take an **on-demand** backup of a cluster (scheduled backups create these
  for you), control its retention, or copy it across namespaces.
- [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) — defines **where**
  backups are stored (S3, S3-compatible, GCS, Azure Blob) and how they are encrypted. A
  cluster references it through `spec.configurations.backups`.

{{% children style="li" depth="1" description="true" %}}

## How the pieces fit together

1. You define an [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) once
   per backup destination, with the credentials for that backend.
2. The cluster references it under `spec.configurations.backups`, optionally with a
   `cronSchedule` and a `retention` — see
   [Configuring automated backups]({{% relref "05-cookbook/02-operating-clusters/09-automated-backups" %}}).
3. Scheduled backups, and any on-demand [SGBackup]({{% relref "06-crd-reference/06-sgbackup" %}})
   you create, write to that storage.

## What to expect

- A backup runs asynchronously; watch its progress and result in the resource status:

  ```bash
  kubectl get sgbackups -n my-cluster -o wide
  ```

- Backups marked with `managedLifecycle: true` are subject to the cluster's automated
  retention policy; others are kept until you delete them.
