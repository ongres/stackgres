---
title: "StackGres Docs"
---

# Overview

> Enterprise Postgres made easy. On Kubernetes

StackGres is a full-stack PostgreSQL distribution for Kubernetes, packed into an easy deployment unit.
With a carefully selected and tuned set of surrounding PostgreSQL components.

An enterprise-grade PostgreSQL stack needs several other ecosystem components and significant tuning.
It's not only PostgreSQL. It requires connection pooling, automatic failover and HA, monitoring,
backups and DR, centralized logging… we have built them all: a Postgres Stack.

Postgres is not just the database. It is also all the ecosystem around it. If Postgres would be the
Linux kernel, we need a PostgreSQL Distribution, surrounding PostgreSQL, to complement it with the
components that are required for a production deployment. This is what we call a PostgreSQL Stack.
And the stack needs to be curated. There are often several software for the same functionality. And
not all is of the same quality or maturity. There are many pros and cons, and they are often not
easy to evaluate. It is better to have an opinionated selection of components, that can be packaged
and configured to work together in a predictable and trusted way.

# The Operator

An Operator is a method of packaging, deploying and managing a Kubernetes
application. Some applications, such as databases, required more hand-holding, and a cloud-native
Postgres requires an operator to provide additional knowledge of how to maintain state and integrate
all the components. The StackGres operator allow to deploy a StackGres cluster using a few custom
resources created by the user.

# The Stack

Curently the stack of StackGres is composed of the following components:

* [PostgreSQL](https://www.postgresql.org/): The world's most advanced open source relational database
* [Patroni](https://github.com/zalando/patroni): The HA solution that relies on kubernetes distributed consensus storage to 
* [WAL-G](https://github.com/wal-g/wal-g): WAL-G is an archival restoration tool for Postgres
* [PgBouncer](http://www.pgbouncer.org/): Lightweight connection pooler for PostgreSQL 
* [PostgreSQL Server Exporter](https://github.com/wrouesnel/postgres_exporter): Prometheus exporter for PostgreSQL server metrics.
* [Envoy](https://www.envoyproxy.io/): open source edge and service proxy, designed for cloud-native applications

