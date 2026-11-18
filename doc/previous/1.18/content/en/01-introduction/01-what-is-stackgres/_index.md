---
title: "What is StackGres?"
weight: 1
url: /intro/about
description: Details about what StackGres is.
---

> StackGres - Enterprise-grade, full-stack PostgreSQL on Kubernetes

StackGres is a full-stack [PostgreSQL](https://www.postgresql.org/) distribution for [Kubernetes](https://kubernetes.io/),
packed into an easy deployment unit, with a carefully selected and tuned set of surrounding PostgreSQL components.

An enterprise-grade PostgreSQL stack needs several other ecosystem components and significant tuning.
It's not only PostgreSQL. It requires connection pooling, automatic failover and HA, monitoring,
backups and DR, centralized logging… we have built them all: a Postgres Stack.

Postgres is not just the database.
It is also all the surrounding ecosystem.
If Postgres was the Linux kernel, we need a PostgreSQL distribution, surrounding PostgreSQL, to complement it with the components that are required for a production deployment.
This is what we call a PostgreSQL Stack.
And the stack needs to be curated.
There are often several software distributions for the same functionality.
And not all are of the same quality or maturity.
There are many pros and cons, and they are often not easy to evaluate.
It is better to have an opinionated selection of components, that can be packaged and configured to work together in a predictable and trusted way.

See a detailed list of [StackGres features]({{% relref "02-features/" %}}).