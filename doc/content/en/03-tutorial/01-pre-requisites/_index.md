---
title: Prerequisites
weight: 1
url: tutorial/prerequisites
---

StackGres runs on Kubernetes, and as such requires an operational K8s cluster to run. This section of the tutorial will
provide basic guidance on how to set up a K8s cluster, just as a reference. But you may run the tutorial on any
K8s-compatible cluster. Please note that all commands in this tutorial are validated with the K8s environments described
here, and may require minor adjustments for different K8s environments. If you need further guidance, please ask in our
[Slack](https://slack.stackgres.io) or [Discord](https://discord.stackgres.io) channels.

You may additionally need:

* **Prometheus (or Prometheus-compatible software) and Grafana**. They are
  additional components, not strictly required. But without Prometheus there will be no automatic monitoring; and
  without Grafana, there will be no integration of the prec-configured StackGres dashboards into the StackGres Web
  Console. Here you will have basic information on how to install them for the tutorial. 

* **An object storage bucket for backups**. StackGres uses object storage (S3, S3-compatible, GCS or Azure Blob) for
  storing the backups. You will need to have one for the tutorial if you want to have backups. Basic guidance will be
  provided here on how to create a bucket, but you may use any compatible object storage bucket.
