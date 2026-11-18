---
title: Remote Cluster
weight: 2
url: /administration/replication/remote
description: This section explains how to set up external replication for clusters.
showToc: true
---

Having a Postgres server replica outside the main 'DC|Zone|Geographic Area' is the next level of High Availability. DBA teams already know stories of companies losing the entire DC because of different physical issues and even more, when working with k8s and having many cloud providers a door is open to the options of setting up StackGres across k8s platform services and migrating from one to other without any vendor restriction. Therefore, StackGres already added the support to create external cluster replicas.

Let's do it!

{{% children style="li" depth="1" description="true" %}}
