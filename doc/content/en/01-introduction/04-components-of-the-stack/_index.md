---
title: "Components of the Stack"
weight: 4
---

# Components of the Stack

Curently the stack of StackGres is composed of the following components:

* [PostgreSQL](https://www.postgresql.org/): The world's most advanced open source relational database
* [Patroni](https://github.com/zalando/patroni): The HA solution that relies on kubernetes distributed consensus storage to 
* [WAL-G](https://github.com/wal-g/wal-g): WAL-G is an archival restoration tool for Postgres
* [PgBouncer](http://www.pgbouncer.org/): Lightweight connection pooler for PostgreSQL 
* [PostgreSQL Server Exporter](https://github.com/wrouesnel/postgres_exporter): Prometheus exporter for PostgreSQL server metrics.
* [Envoy](https://www.envoyproxy.io/): open source edge and service proxy, designed for cloud-native applications
