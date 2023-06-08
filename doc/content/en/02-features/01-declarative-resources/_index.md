---
title: Declarative CRs
weight: 1
url: features/declarative-resources
description: Manage your PostgreSQL cluster using declarative StackGres CRs
---

The StackGres operator is completely managed by Kubernetes custom resources.
There is no need to install any client or additional tool to manage StackGres other than what you already have with `kubectl` or any other Kubernetes API access.
Your requests are represented by the `spec` section of the CRDs, and any result information is provided in the `status` section of the resources in the cluster.

The StackGres CRDs are designed to be very high level, and abstract away (hide) all Postgres complexities.
With StackGres, if you know how to use `kubectl` and how to define CRDs, you have become an expert Postgres user, as well.

The ability to define StackGres definitions and operations in CRDs enables you to define all your Postgres clusters as Infrastructure as Code (IaC), and to version-control e.g. in Git.
This enables a GitOps way of configuration management, where your clusters can be managed automatically when you commit changes to the CRDs in your version control.
In general, this way of resource definition allows for a very flexible and extensible approach.

Have a look at the [CRD Reference]({{% relref "06-crd-reference" %}}) to learn about the structure of the StackGres resources.

Have a look at the [Getting Started]({{% relref "03-demo-quickstart" %}}) guide to get started with a simple StackGres installation.

> **Note:** If you prefer a visual UI over YAML files and the command line, note that every single action that you can query or perform via CRDs is also possible via the web console.
> Likewise, any action performed in the web console will automatically be reflected in the CRDs.
> The choice is yours.