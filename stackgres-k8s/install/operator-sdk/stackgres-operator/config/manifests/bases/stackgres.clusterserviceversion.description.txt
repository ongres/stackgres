
[StackGres](https://stackgres.io) is the **Stack** required for enterprise production Post**Gres**. A fully-featured platform to run Postgres on Kubernetes. Fully Open Source, StackGres supports both a declarative approach suitable for GitOps workflows and a complete Web Console for the best user experience.

Built by [OnGres](https://ongres.com) ("**On** Post**Gres**"), StackGres requires little to no prior Postgres experience. StackGres can perform fully automated deployments; fully automated database operations ("Day 2 operations") and comes with advanced database tuning by default. Yet remaining highly customizable for Postgres expert DBAs.

[StackGres features](https://stackgres.io/features/) include, among others:

* **High Availability with automated failover**. StackGres relies on [Patroni](https://github.com/zalando/patroni), and its built-in and fully automatic.
* **Integrated connection pooling**. Built-in, by default, like it should be for production workloads.
* **Automatic backups with lifecycle policies**. Backup your clusters automatically to any object store. Apply retention policies. Restoration supports PITR.
* **Advanced replication modes**, including async, sync and group replication. It also supports cascading replication and standby clusters on separate Kubernetes clusters for disaster recovery.
* **More than 150 Postgres extensions**. The Postgres platform with [the largest number of extensions in the world](https://stackgres.io/extensions/). With new extensions added continuously.
* **Observability**. Fully integrated with the Prometheus stack. Includes pre-defined, Postgres-specific dashboards and alerts.
* **Fully-featured Web Console**. Perform any operation from the Web Console. Supports SSO, fine-grained RBAC and a REST API.
* **Distributed Logs**. StackGres developed a mechanism to ship logs from all pods to a central log server, managed by StackGres, that store logs in Postgres. Query your logs with SQL or from the Web Console!
* **Automated Day 2 Operations**. Minor and major version upgrades, container upgrades, controlled restart, vacuum, repack, even benchmarks!
* **Expertly tuned by default**. From the creators of [CONF](https://postgresqlco.nf), StackGres pre-tunes your Postgres servers with more than 40 parameters tuned by default.
* **100% Open Source**. No "premium version with advanced features", no production usage restrictions. Just Open Source.
* **[24/7 Support](https://stackgres.io/pricing/) Available from OnGres**

## Installation and documentation

Installation:
* For a quick test, you can follow our [quickstart](https://stackgres.io/doc/latest/demo/quickstart/).
* [Production installations](https://stackgres.io/doc/latest/install/).

All the documentation is available at [stackgres.io/doc](https://stackgres.io/doc/latest/install/).

Join the [Slack](https://slack.stackgres.io) and/or [Discord](https://discord.stackgres.io) Public Communities for Community support.
