---
title: Day-2 Operations
weight: 10
url: /features/day-2-operations
description: "Automated day-2 operations such as minor & major PostgreSQL version upgrades, container upgrades, controlled restarts, vacuum, or repack"
---

StackGres fully manages your Postgres clusters in a production-ready manner.
It comes with day-2 operation functionalities such as minor & major PostgreSQL version upgrades, container upgrades, controlled restarts, vacuum, or repack, that can be used in an easy and safe way.

Management operations such as version upgrades or restarts can easily create stressful situations, that's why a well-engineered database operator should implement these operations in a reasonable way.
StackGres performs the day-2 operations in a controlled, production-grade manner.

In general, the engineers define the desired target state in the StackGres CRDs, apply changes via the Kubernetes API (or alternatively the Web Console), and the StackGres operator takes care of the rest.
This approach minimizes careless mistakes from the user's side since no procedural steps are required but declarative definitions.
The StackGres CRDs provide a type-safe way to define resources, configurations, and cluster actions, and are understood and validated by the StackGres operator.

Depending on the desired change, StackGres performs changes in a reasonable way, with production-readiness in mind.
This means, for example, that required Postgres cluster restarts are not just performed right away (and potentially jeopardize database availability), but in a controlled manner.

StackGres aims to provide the best combination of a DBA expert and Kubernetes operator.

## Available Operations

For detailed guides on each operation, see:

- [Database Operations Guide]({{% relref "04-administration-guide/06-database-operations/01-benchmarking" %}})
- [SGDbOps Reference]({{% relref "06-crd-reference/08-sgdbops" %}})