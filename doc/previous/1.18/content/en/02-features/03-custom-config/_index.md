---
title: Custom Configuration
weight: 3
url: /features/custom-config
description: Provide custom configuration on top of vanilla PostgreSQL
---

StackGres is built with vanilla Postgres and provides Postgres instances with no restrictions.
You're able to provide custom configurations and use any further customizations that you can do on your own Postgres installation.

## Full postgres User Access

There are no limited pseudo users in StackGres.
Instead, you get access to the `postgres` user (the "root" user in Postgres) with its maximum privileges.
You own it without any caveats.

## Custom Configuration

StackGres allows advanced Postgres users to further customize the components and configurations.
The configurations are backed by CRDs and fully validated, so there is not a simple ConfigMap that may break your cluster if you set it wrongly.

The operator creates default configuration custom resources if they are not specified. It also allows setting the configurations inline inside the cluster CRD, including those components that do not have a separate CRD like Patroni or the Postgres Exporter.

Have a look at the [Configuration Guide]({{% relref "04-administration-guide/04-configuration" %}}) for a deep dive in how to tune Postgres or connection pool configurations.

As for the other Kubernetes resources, you can customize the services exposed, the pod's labels and node tolerations, among many others.

In general, StackGres lets you be in full control.