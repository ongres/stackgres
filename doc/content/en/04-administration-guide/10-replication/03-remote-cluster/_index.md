---
title: Remote Cluster
weight: 1
url: /administration/replication/remote
description: This section explains the features in Stackgres to setup external replication clusters.
showToc: true
---

Having a Postgres server replica outside the main 'DC|Zone|Geografic Area' is the next level of High Avaliability. DBA teams already know stories of companies losing the entire DC because of different physical issues and even more when working with k8s and having many cloud providers a door is open to the options of setting up Stackgres across k8s platform services and migrating from one to other without any vendor restriction. Therefore, Stackgres already added the support to create external cluster replicas.

Lets do it!

{{% children style="li" depth="1" description="true" %}}
