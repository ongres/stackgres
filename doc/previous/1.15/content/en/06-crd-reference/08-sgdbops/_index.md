---
title: SGDbOps
weight: 8
url: /reference/crd/sgdbops
description: Details about SGDbOps
showToc: true
---

___
**Kind:** SGDbOps

**listKind:** SGDbOpsList

**plural:** sgdbops

**singular:** sgdbops

**shortNames** sgdo
___

The `SGDbOps` custom resource represents database operations that are performed on a Postgres cluster.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: benchmark
spec:
 sgCluster: my-cluster
 op: benchmark
 maxRetries: 1
 benchmark:
   type: pgbench
   pgbench:
     databaseSize: 1Gi
     duration: P0DT0H10M0S
     concurrentClients: 10
     threads: 10
   connectionType: primary-service
```





<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGDbOps</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGDbOps.spec
<sup><sup>[↩ Parent](#sgdbops)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>op</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The kind of operation that will be performed on the SGCluster. Available operations are:

* `benchmark`: run a benchmark on the specified SGCluster and report the results in the status.
* `vacuum`: perform a [vacuum](https://www.postgresql.org/docs/current/sql-vacuum.html) operation on the specified SGCluster.
* `repack`: run [`pg_repack`](https://github.com/reorg/pg_repack) command on the specified SGCluster.
* `majorVersionUpgrade`: perform a major version upgrade of PostgreSQL using [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command.
* `restart`: perform a restart of the cluster.
* `minorVersionUpgrade`: perform a minor version upgrade of PostgreSQL.
* `securityUpgrade`: perform a security upgrade of the cluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgCluster</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of SGCluster on which the operation will be performed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmark">benchmark</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of the benchmark
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecmajorversionupgrade">majorVersionUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of major version upgrade (see also [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>maxRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The maximum number of retries the operation is allowed to do after a failure.

A value of `0` (zero) means no retries are made. Defaults to: `0`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecminorversionupgrade">minorVersionUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of minor version upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecrepack">repack</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of [`pg_repack`](https://github.com/reorg/pg_repack) command
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecrestart">restart</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of restart
<br/>
        </td>
      </tr>
      <tr>
        <td><b>runAt</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date, that holds UTC scheduled date of the operation execution.

If not specified or if the date it's in the past, it will be interpreted ASAP.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecscheduling">scheduling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod custom node scheduling and affinity configuration<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecsecurityupgrade">securityUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of security upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b>timeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies a timeout after which the operation execution will be canceled.

If the operation can not be performed due to timeout expiration, the condition `Failed` will have a status of `True` and the reason will be `OperationTimedOut`.

If not specified the operation will never fail for timeout expiration.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecvacuum">vacuum</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of [vacuum](https://www.postgresql.org/docs/current/sql-vacuum.html) operation
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.benchmark
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of the benchmark


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The type of benchmark that will be performed on the SGCluster. Available benchmarks are:

* `pgbench`: run [pgbench](https://www.postgresql.org/docs/current/pgbench.html) on the specified SGCluster and report the results in the status.
* `sampling`: samples real queries and store them in the SGDbOps status in order to be used by a `pgbench` benchmark using `replay` mode.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>connectionType</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Specify the service where the benchmark will connect to:

* `primary-service`: Connect to the primary service
* `replicas-service`: Connect to the replicas service
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkcredentials">credentials</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The credentials of the user that will be used by the benchmark<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          When specified will indicate the database where the benchmark will run upon.

If not specified a target database with a random name will be created and removed after the benchmark completes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbench">pgbench</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of [pgbench](https://www.postgresql.org/docs/current/pgbench.html) benchmark
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarksampling">sampling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Configuration of sampling benchmark.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.benchmark.credentials
<sup><sup>[↩ Parent](#sgdbopsspecbenchmark)</sup></sup>

The credentials of the user that will be used by the benchmark

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkcredentialspassword">password</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The password that will be used by the benchmark

If not specified the default superuser password will be used.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkcredentialsusername">username</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The username that will be used by the benchmark.

If not specified the default superuser username (by default postgres) will be used.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.credentials.password
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkcredentials)</sup></sup>

The password that will be used by the benchmark

If not specified the default superuser password will be used.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret key where the password is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the password is stored.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.credentials.username
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkcredentials)</sup></sup>

The username that will be used by the benchmark.

If not specified the default superuser username (by default postgres) will be used.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret key where the username is stored.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Secret name where the username is stored.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.benchmark.pgbench
<sup><sup>[↩ Parent](#sgdbopsspecbenchmark)</sup></sup>

Configuration of [pgbench](https://www.postgresql.org/docs/current/pgbench.html) benchmark


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>databaseSize</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Size of the database to generate. This size is specified either in Mebibytes, Gibibytes or Tebibytes (multiples of 2^20, 2^30 or 2^40, respectively).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>duration</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how long the benchmark will run.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>concurrentClients</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of clients simulated, that is, number of concurrent database sessions. Defaults to: `1`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustom">custom</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          This section allow to configure custom SQL for initialization and scripts used by pgbench.<br/>
        </td>
      </tr>
      <tr>
        <td><b>fillfactor</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Create the pgbench_accounts, pgbench_tellers and pgbench_branches tables with the given fillfactor. Default is 100.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>foreignKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Create foreign key constraints between the standard tables. (This option only take effect if `custom.initiailization` is not specified).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initSteps</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Perform just a selected set of the normal initialization steps. init_steps specifies the initialization steps to be performed, using one character per step. Each step is invoked in the specified order. The default is dtgvp. The available steps are:

* `d` (Drop): Drop any existing pgbench tables.
* `t` (create Tables): Create the tables used by the standard pgbench scenario, namely pgbench_accounts, pgbench_branches, pgbench_history, and pgbench_tellers.
* `g` or `G` (Generate data, client-side or server-side): Generate data and load it into the standard tables, replacing any data already present.
  With `g` (client-side data generation), data is generated in pgbench client and then sent to the server. This uses the client/server bandwidth extensively through a COPY. pgbench uses the FREEZE option with version 14 or later of PostgreSQL to speed up subsequent VACUUM, unless partitions are enabled. Using g causes logging to print one message every 100,000 rows while generating data for the pgbench_accounts table.
  With `G` (server-side data generation), only small queries are sent from the pgbench client and then data is actually generated in the server. No significant bandwidth is required for this variant, but the server will do more work. Using G causes logging not to print any progress message while generating data.
  The default initialization behavior uses client-side data generation (equivalent to g).
* `v` (Vacuum): Invoke VACUUM on the standard tables.
* `p` (create Primary keys): Create primary key indexes on the standard tables.
* `f` (create Foreign keys): Create foreign key constraints between the standard tables. (Note that this step is not performed by default.)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>mode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The pgbench benchmark type:

* `tpcb-like`: The benchmark is inspired by the [TPC-B benchmark](https://www.tpc.org/TPC_Documents_Latest_Versions/TPC-B_v2.0.0.pdf). It is the default mode when `connectionType` is set to `primary-service`.
* `select-only`: The `tpcb-like` but only using SELECTs commands. It is the default mode when `connectionType` is set to `replicas-service`.
* `custom`: will use the scripts in the `custom` section to initialize and and run commands for the benchmark.
* `replay`: will replay the sampled queries of a sampling benchmark SGDbOps. If the `custom` section is specified it will be used instead. Queries can be referenced setting `custom.scripts.replay` to the index of the query in the sampling benchmark SGDbOps's status (index start from 0).

See also https://www.postgresql.org/docs/current/pgbench.html#TRANSACTIONS-AND-SCRIPTS
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noVacuum</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Perform no vacuuming during initialization. (This option suppresses the `v` initialization step, even if it was specified in `initSteps`.)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>partitionMethod</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Create a partitioned pgbench_accounts table with the specified method. Expected values are `range` or `hash`. This option requires that partitions is set to non-zero. If unspecified, default is `range`. (This option only take effect if `custom.initiailization` is not specified).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>partitions</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Create a partitioned pgbench_accounts table with the specified number of partitions of nearly equal size for the scaled number of accounts. Default is 0, meaning no partitioning. (This option only take effect if `custom.initiailization` is not specified).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>queryMode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Protocol to use for submitting queries to the server:

* `simple`: use simple query protocol.
* `extended`: use extended query protocol.
* `prepared`: use extended query protocol with prepared statements.

In the prepared mode, pgbench reuses the parse analysis result starting from the second query iteration, so pgbench runs faster than in other modes.

The default is `simple` query protocol. See also https://www.postgresql.org/docs/current/protocol.html
<br/>
        </td>
      </tr>
      <tr>
        <td><b>samplingRate</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          Sampling rate, used when collecting data, to reduce the amount of collected data. If this option is given, only the specified fraction of transactions are collected. 1.0 means all transactions will be logged, 0.05 means only 5% of the transactions will be logged.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>samplingSGDbOps</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          benchmark SGDbOps of type sampling that will be used to replay sampled queries.<br/>
        </td>
      </tr>
      <tr>
        <td><b>threads</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of worker threads within pgbench. Using more than one thread can be helpful on multi-CPU machines. Clients are distributed as evenly as possible among available threads. Default is `1`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unloggedTables</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Create all tables as unlogged tables, rather than permanent tables. (This option only take effect if `custom.initiailization` is not specified).
<br/>
        </td>
      </tr>
      <tr>
        <td><b>usePreparedStatements</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          **Deprecated** this field is ignored, use `queryMode` instead.

Use extended query protocol with prepared statements. Defaults to: `false`.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbench)</sup></sup>

This section allow to configure custom SQL for initialization and scripts used by pgbench.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustominitialization">initialization</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The custom SQL for initialization that will be executed in place of pgbench default initialization.

If not specified the default pgbench initialization will be performed instead.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustomscriptsindex">scripts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The custom SQL scripts that will be executed by pgbench during the benchmark instead of default pgbench scripts<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.initialization
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustom)</sup></sup>

The custom SQL for initialization that will be executed in place of pgbench default initialization.

If not specified the default pgbench initialization will be performed instead.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>script</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfrom">scriptFrom</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.initialization.scriptFrom
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustominitialization)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfromconfigmapkeyref">configMapKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfromsecretkeyref">secretKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.initialization.scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key name within the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.initialization.scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustominitializationscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index]
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustom)</sup></sup>

A custom SQL script that will be executed by pgbench during the benchmark instead of default pgbench scripts

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>builtin</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the builtin script to use. See https://www.postgresql.org/docs/current/pgbench.html#PGBENCH-OPTION-BUILTIN

When specified fields `replay`, `script` and `scriptFrom` must not be set.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>replay</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The index of the query in the sampling benchmark SGDbOps's status (index start from 0).

When specified fields `builtin`, `script` and `scriptFrom` must not be set.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>script</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfrom">scriptFrom</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The weight of this custom SQL script.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index].scriptFrom
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustomscriptsindex)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfromconfigmapkeyref">configMapKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfromsecretkeyref">secretKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index].scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key name within the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.benchmark.pgbench.custom.scripts[index].scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgdbopsspecbenchmarkpgbenchcustomscriptsindexscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.benchmark.sampling
<sup><sup>[↩ Parent](#sgdbopsspecbenchmark)</sup></sup>

Configuration of sampling benchmark.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>samplingDuration</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how long will last the sampling of real queries that will be replayed later.<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetDatabase</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target database to be sampled. By default `postgres`.

The benchmark database will be used to store the sampled queries but user must specify a target database to be sampled in the `sampling` section.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topQueriesCollectDuration</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how long the to wait before selecting top queries in order to collect enough stats.<br/>
        </td>
      </tr>
      <tr>
        <td><b>customTopQueriesQuery</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The query used to select top queries. Will be ignored if `mode` is not set to `custom`.

The query must return at most 2 columns:

* First column returned by the query must be a column holding the query identifier, also available in pg_stat_activity (column `query_id`) and pg_stat_statements (column `queryid`).
* Second column is optional and, if returned, must hold a json object containing only text keys and values stat will be used to generate the stats.

See also:

* https://www.postgresql.org/docs/current/monitoring-stats.html#MONITORING-PG-STAT-ACTIVITY-VIEW
* https://www.postgresql.org/docs/current/pgstatstatements.html#PGSTATSTATEMENTS-PG-STAT-STATEMENTS
<br/>
        </td>
      </tr>
      <tr>
        <td><b>mode</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The mode used to select the top queries used for sampling:

* `time`: The top queries will be selected among the most slow queries.
* `calls`: The top queries will be selected among the most called queries.
* `custom`: The `customTopQueriesQuery` will be used to select top queries.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>omitTopQueriesInStatus</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When `true` omit to include the top queries stats in the SGDbOps status. By default `false`.<br/>
        </td>
      </tr>
      <tr>
        <td><b>queries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Number of sampled queries to include in the result. By default `10`.<br/>
        </td>
      </tr>
      <tr>
        <td><b>samplingMinInterval</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum number of microseconds the sampler will wait between each sample is taken. By default `10000` (10 milliseconds).<br/>
        </td>
      </tr>
      <tr>
        <td><b>topQueriesFilter</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Regular expression for filtering representative statements when selecting top queries. Will be ignored if `mode` is set to `custom`. By default is `^ *(with|select) `. See https://www.postgresql.org/docs/current/functions-matching.html#FUNCTIONS-POSIX-REGEXP<br/>
        </td>
      </tr>
      <tr>
        <td><b>topQueriesMin</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Minimum number of queries to consider as part of the top queries. By default `5`.<br/>
        </td>
      </tr>
      <tr>
        <td><b>topQueriesPercentile</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Percentile of queries to consider as part of the top queries. Will be ignored if `mode` is set to `custom`. By default `95`.<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of major version upgrade (see also [`pg_upgrade`](https://www.postgresql.org/docs/current/pgupgrade.html) command)


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>backupPath</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The path were the backup is stored. If not set this field is filled up by the operator.

When provided will indicate were the backups and WAL files will be stored.

The path should be different from the current `.spec.configurations.backups[].path` value for the target `SGCluster`
  in order to avoid mixing WAL files of two distinct major versions of postgres.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>check</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true does some checks to see if the cluster can perform a major version upgrade without changing any data. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>clone</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true use efficient file cloning (also known as "reflinks" on some systems) instead of copying files to the new cluster.
This can result in near-instantaneous copying of the data files, giving the speed advantages of `link` while leaving the old
  cluster untouched. This option is mutually exclusive with `link`. Defaults to: `false`.

File cloning is only supported on some operating systems and file systems. If it is selected but not supported, the pg_upgrade
  run will error. At present, it is supported on Linux (kernel 4.5 or later) with Btrfs and XFS (on file systems created with
  reflink support), and on macOS with APFS.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>link</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true use hard links instead of copying files to the new cluster. This option is mutually exclusive with `clone`. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecmajorversionupgradepostgresextensionsindex">postgresExtensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A major version upgrade can not be performed if a required extension is not present for the target major version of the upgrade.
In those cases you will have to provide the target extension version of the extension for the target major version of postgres.
Beware that in some cases it is not possible to upgrade an extension alongside postgres. This is the case for PostGIS or timescaledb.
 In such cases you will have to upgrade the extension before or after the major version upgrade. Please make sure you read the
 documentation of each extension in order to understand if it is possible to upgrade it during a major version upgrade of postgres.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target postgres version that must have the same major version of the target SGCluster.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sgPostgresConfig</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres config that must have the same major version of the target postgres version.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecmajorversionupgradetoinstallpostgresextensionsindex">toInstallPostgresExtensions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The list of Postgres extensions to install.

**This section is filled by the operator.**
<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.majorVersionUpgrade.postgresExtensions[index]
<sup><sup>[↩ Parent](#sgdbopsspecmajorversionupgrade)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the extension to deploy.<br/>
        </td>
      </tr>
      <tr>
        <td><b>publisher</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The id of the publisher of the extension to deploy. If not specified `com.ongres` will be used by default.<br/>
          <br/>
            <i>Default</i>: com.ongres<br/>
        </td>
      </tr>
      <tr>
        <td><b>repository</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The repository base URL from where to obtain the extension to deploy.

**This section is filled by the operator.**
<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The version of the extension to deploy. If not specified version of `stable` channel will be used by default and if only a version is available that one will be used.<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.majorVersionUpgrade.toInstallPostgresExtensions[index]
<sup><sup>[↩ Parent](#sgdbopsspecmajorversionupgrade)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresVersion</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres major version of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>publisher</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The id of the publisher of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>repository</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The repository base URL from where the extension will be installed from.<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The version of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>build</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The build version of the extension to install.<br/>
        </td>
      </tr>
      <tr>
        <td><b>extraMounts</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The extra mounts of the extension to install.<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of minor version upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>method</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The method used to perform the minor version upgrade operation. Available methods are:

* `InPlace`: the in-place method does not require more resources than those that are available.
  In case only an instance of the StackGres cluster is present this mean the service disruption will
  last longer so we encourage use the reduced impact restart and especially for a production environment.
* `ReducedImpact`: this procedure is the same as the in-place method but require additional
  resources in order to spawn a new updated replica that will be removed when the procedure completes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>postgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The target postgres version that must have the same major version of the target SGCluster.
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.repack
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of [`pg_repack`](https://github.com/reorg/pg_repack) command


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecrepackdatabasesindex">databases</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          List of database to vacuum or repack, don't specify to select all databases
<br/>
        </td>
      </tr>
      <tr>
        <td><b>excludeExtension</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true don't repack tables which belong to specific extension. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noAnalyze</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true don't analyze at end. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noKillBackend</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true don't kill other backends when timed out. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noOrder</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true do vacuum full instead of cluster. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>waitTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If specified, an ISO 8601 duration format `PnDTnHnMn.nS` to set a timeout to cancel other backends on conflict.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.repack.databases[index]
<sup><sup>[↩ Parent](#sgdbopsspecrepack)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          the name of the database<br/>
        </td>
      </tr>
      <tr>
        <td><b>excludeExtension</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true don't repack tables which belong to specific extension. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noAnalyze</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true don't analyze at end. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noKillBackend</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true don't kill other backends when timed out. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>noOrder</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true do vacuum full instead of cluster. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>waitTimeout</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If specified, an ISO 8601 duration format `PnDTnHnMn.nS` to set a timeout to cancel other backends on conflict.
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.restart
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of restart


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>method</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The method used to perform the restart operation. Available methods are:

* `InPlace`: the in-place method does not require more resources than those that are available.
  In case only an instance of the StackGres cluster is present this mean the service disruption will
  last longer so we encourage use the reduced impact restart and especially for a production environment.
* `ReducedImpact`: this procedure is the same as the in-place method but require additional
  resources in order to spawn a new updated replica that will be removed when the procedure completes.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>onlyPendingRestart</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          By default all Pods are restarted. Setting this option to `true` allow to restart only those Pods which
  are in pending restart state as detected by the operation. Defaults to: `false`.
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.scheduling
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Pod custom node scheduling and affinity configuration

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinity">nodeAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#nodeaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>nodeSelector</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          NodeSelector is a selector which must be true for the pod to fit on a node. Selector which must match a node's labels for the pod to be scheduled on that node. More info: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinity">podAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinity">podAntiAffinity</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podantiaffinity-v1-core<br/>
        </td>
      </tr>
      <tr>
        <td><b>priorityClassName</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          If specified, indicates the pod's priority. "system-node-critical" and "system-cluster-critical" are two special keywords which indicate the highest priorities with the former being the highest priority. Any other name must be defined by creating a PriorityClass object with that name. If not specified, the pod priority will be default or zero if there is no default.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingtolerationsindex">tolerations</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If specified, the pod's tolerations.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#toleration-v1-core<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.scheduling.nodeAffinity
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

Node affinity is a group of node affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#nodeaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node matches the corresponding matchExpressions; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinity)</sup></sup>

An empty preferred scheduling term matches all objects with implicit weight 0 (i.e. it's a no-op). A null preferred scheduling term matches no objects (i.e. is also a no-op).

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference">preference</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Weight associated with matching the corresponding nodeSelectorTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreferencematchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].preference.matchFields[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinitypreferredduringschedulingignoredduringexecutionindexpreference)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinity)</sup></sup>

A node selector represents the union of the results of one or more label queries over a set of nodes; that is, it represents the OR of the selectors represented by the node selector terms.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex">nodeSelectorTerms</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Required. A list of node selector terms. The terms are ORed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecution)</sup></sup>

A null or empty node selector term matches no objects. The requirements of them are ANDed. The TopologySelectorTerm type implements a subset of the NodeSelectorTerm.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's labels.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindexmatchfieldsindex">matchFields</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of node selector requirements by node's fields.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[index].matchFields[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingnodeaffinityrequiredduringschedulingignoredduringexecutionnodeselectortermsindex)</sup></sup>

A node selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists, DoesNotExist. Gt, and Lt.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          An array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. If the operator is Gt or Lt, the values array must have a single element, which will be interpreted as an integer. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.scheduling.podAffinity
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

Pod affinity is a group of inter pod affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If the affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.scheduling.podAntiAffinity
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

Pod anti affinity is a group of inter pod anti affinity scheduling rules.

See https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#podantiaffinity-v1-core

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex">preferredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The scheduler will prefer to schedule pods to nodes that satisfy the anti-affinity expressions specified by this field, but it may choose a node that violates one or more of the expressions. The node that is most preferred is the one with the greatest sum of weights, i.e. for each node that meets all of the scheduling requirements (resource request, requiredDuringScheduling anti-affinity expressions, etc.), compute a sum by iterating through the elements of this field and adding "weight" to the sum if the node has pods which matches the corresponding podAffinityTerm; the node(s) with the highest sum are the most preferred.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex">requiredDuringSchedulingIgnoredDuringExecution</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          If the anti-affinity requirements specified by this field are not met at scheduling time, the pod will not be scheduled onto the node. If the anti-affinity requirements specified by this field cease to be met at some point during pod execution (e.g. due to a pod label update), the system may or may not try to eventually evict the pod from its node. When there are multiple elements, the lists of nodes corresponding to each podAffinityTerm are intersected, i.e. all terms must be satisfied.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinity)</sup></sup>

The weights of all of the matched WeightedPodAffinityTerm fields are added per-node to find the most preferred node(s)

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm">podAffinityTerm</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running<br/>
        </td>
      </tr>
      <tr>
        <td><b>weight</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          weight associated with matching the corresponding podAffinityTerm, in the range 1-100.<br/>
          <br/>
            <i>Format</i>: int32<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindex)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinityterm)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[index].podAffinityTerm.namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinitypreferredduringschedulingignoredduringexecutionindexpodaffinitytermnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinity)</sup></sup>

Defines a set of pods (namely those matching the labelSelector relative to the given namespace(s)) that this pod should be co-located (affinity) or not co-located (anti-affinity) with, where co-located is defined as running on a node whose value of the label with key <topologyKey> matches that of any node on which a pod of the set of pods is running

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>topologyKey</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          This pod should be co-located (affinity) or not co-located (anti-affinity) with the pods matching the labelSelector in the specified namespaces, where co-located is defined as running on a node whose value of the label with key topologyKey matches that of any node on which any of the selected pods is running. Empty topologyKey is not allowed.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector">labelSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key in (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MatchLabelKeys and LabelSelector. Also, MatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b>mismatchLabelKeys</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          MismatchLabelKeys is a set of pod label keys to select which pods will be taken into consideration. The keys are used to lookup values from the incoming pod labels, those key-value labels are merged with `LabelSelector` as `key notin (value)` to select the group of existing pods which pods will be taken into consideration for the incoming pod's pod (anti) affinity. Keys that don't exist in the incoming pod labels will be ignored. The default value is empty. The same key is forbidden to exist in both MismatchLabelKeys and LabelSelector. Also, MismatchLabelKeys cannot be set when LabelSelector isn't set. This is an alpha field and requires enabling MatchLabelKeysInPodAffinity feature gate.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector">namespaceSelector</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.<br/>
        </td>
      </tr>
      <tr>
        <td><b>namespaces</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          namespaces specifies a static list of namespace names that the term applies to. The term is applied to the union of the namespaces listed in this field and the ones selected by namespaceSelector. null or empty namespaces list and null namespaceSelector means "this pod's namespace".<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].labelSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexlabelselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindex)</sup></sup>

A label selector is a label query over a set of resources. The result of matchLabels and matchExpressions are ANDed. An empty label selector matches all objects. A null label selector matches no objects.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselectormatchexpressionsindex">matchExpressions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          matchExpressions is a list of label selector requirements. The requirements are ANDed.<br/>
        </td>
      </tr>
      <tr>
        <td><b>matchLabels</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          matchLabels is a map of {key,value} pairs. A single {key,value} in the matchLabels map is equivalent to an element of matchExpressions, whose key field is "key", the operator is "In", and the values array contains only "value". The requirements are ANDed.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.spec.scheduling.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[index].namespaceSelector.matchExpressions[index]
<sup><sup>[↩ Parent](#sgdbopsspecschedulingpodantiaffinityrequiredduringschedulingignoredduringexecutionindexnamespaceselector)</sup></sup>

A label selector requirement is a selector that contains values, a key, and an operator that relates the key and values.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          key is the label key that the selector applies to.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          operator represents a key's relationship to a set of values. Valid operators are In, NotIn, Exists and DoesNotExist.<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          values is an array of string values. If the operator is In or NotIn, the values array must be non-empty. If the operator is Exists or DoesNotExist, the values array must be empty. This array is replaced during a strategic merge patch.<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.scheduling.tolerations[index]
<sup><sup>[↩ Parent](#sgdbopsspecscheduling)</sup></sup>

The pod this Toleration is attached to tolerates any taint that matches the triple <key,value,effect> using the matching operator <operator>.

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>effect</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Effect indicates the taint effect to match. Empty means match all taint effects. When specified, allowed values are NoSchedule, PreferNoSchedule and NoExecute.<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Key is the taint key that the toleration applies to. Empty means match all taint keys. If the key is empty, operator must be Exists; this combination means to match all values and all keys.<br/>
        </td>
      </tr>
      <tr>
        <td><b>operator</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Operator represents a key's relationship to the value. Valid operators are Exists and Equal. Defaults to Equal. Exists is equivalent to wildcard for value, so that a pod can tolerate all taints of a particular category.<br/>
        </td>
      </tr>
      <tr>
        <td><b>tolerationSeconds</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          TolerationSeconds represents the period of time the toleration (which must be of effect NoExecute, otherwise this field is ignored) tolerates the taint. By default, it is not set, which means tolerate the taint forever (do not evict). Zero and negative values will be treated as 0 (evict immediately) by the system.<br/>
          <br/>
            <i>Format</i>: int64<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Value is the taint value the toleration matches to. If the operator is Exists, the value should be empty, otherwise just a regular string.<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.securityUpgrade
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of security upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>method</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The method used to perform the security upgrade operation. Available methods are:

* `InPlace`: the in-place method does not require more resources than those that are available.
  In case only an instance of the StackGres cluster is present this mean the service disruption will
  last longer so we encourage use the reduced impact restart and especially for a production environment.
* `ReducedImpact`: this procedure is the same as the in-place method but require additional
  resources in order to spawn a new updated replica that will be removed when the procedure completes.
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.spec.vacuum
<sup><sup>[↩ Parent](#sgdbopsspec)</sup></sup>

Configuration of [vacuum](https://www.postgresql.org/docs/current/sql-vacuum.html) operation


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>analyze</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true, updates statistics used by the planner to determine the most efficient way to execute a query. Defaults to: `true`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsspecvacuumdatabasesindex">databases</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          List of databases to vacuum or repack, don't specify to select all databases
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disablePageSkipping</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Normally, VACUUM will skip pages based on the visibility map. Pages where all tuples are known to be frozen can always be
  skipped, and those where all tuples are known to be visible to all transactions may be skipped except when performing an
  aggressive vacuum. Furthermore, except when performing an aggressive vacuum, some pages may be skipped in order to avoid
  waiting for other sessions to finish using them. This option disables all page-skipping behavior, and is intended to be
  used only when the contents of the visibility map are suspect, which should happen only if there is a hardware or
  software issue causing database corruption. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>freeze</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true selects aggressive "freezing" of tuples. Specifying FREEZE is equivalent to performing VACUUM with the
  vacuum_freeze_min_age and vacuum_freeze_table_age parameters set to zero. Aggressive freezing is always performed
  when the table is rewritten, so this option is redundant when FULL is specified. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>full</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true selects "full" vacuum, which can reclaim more space, but takes much longer and exclusively locks the table.
This method also requires extra disk space, since it writes a new copy of the table and doesn't release the old copy
  until the operation is complete. Usually this should only be used when a significant amount of space needs to be
  reclaimed from within the table. Defaults to: `false`.
<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.spec.vacuum.databases[index]
<sup><sup>[↩ Parent](#sgdbopsspecvacuum)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          the name of the database<br/>
        </td>
      </tr>
      <tr>
        <td><b>analyze</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true, updates statistics used by the planner to determine the most efficient way to execute a query. Defaults to: `true`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>disablePageSkipping</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          Normally, VACUUM will skip pages based on the visibility map. Pages where all tuples are known to be frozen can always be
  skipped, and those where all tuples are known to be visible to all transactions may be skipped except when performing an
  aggressive vacuum. Furthermore, except when performing an aggressive vacuum, some pages may be skipped in order to avoid
  waiting for other sessions to finish using them. This option disables all page-skipping behavior, and is intended to be
  used only when the contents of the visibility map are suspect, which should happen only if there is a hardware or
  software issue causing database corruption. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>freeze</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true selects aggressive "freezing" of tuples. Specifying FREEZE is equivalent to performing VACUUM with the
  vacuum_freeze_min_age and vacuum_freeze_table_age parameters set to zero. Aggressive freezing is always performed
  when the table is rewritten, so this option is redundant when FULL is specified. Defaults to: `false`.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>full</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If true selects "full" vacuum, which can reclaim more space, but takes much longer and exclusively locks the table.
This method also requires extra disk space, since it writes a new copy of the table and doesn't release the old copy
  until the operation is complete. Usually this should only be used when a significant amount of space needs to be
  reclaimed from within the table. Defaults to: `false`.
<br/>
        </td>
      </tr></tbody>
</table>

## SGDbOps.status
<sup><sup>[↩ Parent](#sgdbops)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmark">benchmark</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of the benchmark
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusconditionsindex">conditions</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Possible conditions are:

* Running: to indicate when the operation is actually running
* Completed: to indicate when the operation has completed successfully
* Failed: to indicate when the operation has failed
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusmajorversionupgrade">majorVersionUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of a major version upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusminorversionupgrade">minorVersionUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of a minor version upgrade
<br/>
        </td>
      </tr>
      <tr>
        <td><b>opRetries</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of retries performed by the operation
<br/>
        </td>
      </tr>
      <tr>
        <td><b>opStarted</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The ISO 8601 timestamp of when the operation started running
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusrestart">restart</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of a restart
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatussecurityupgrade">securityUpgrade</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of a security upgrade
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.status.benchmark
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

The results of the benchmark


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbench">pgbench</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of the pgbench benchmark<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarksampling">sampling</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The results of the sampling benchmark<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.status.benchmark.pgbench
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmark)</sup></sup>

The results of the pgbench benchmark

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hdrHistogram</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Compressed and base 64 encoded HdrHistogram<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchlatency">latency</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The latency results of the pgbench benchmark
<br/>
        </td>
      </tr>
      <tr>
        <td><b>scaleFactor</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          The scale factor used to run pgbench (`--scale`).
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchstatementsindex">statements</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          Average per-statement latency (execution time from the perspective of the client) of each command after the benchmark finishes<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchtransactionspersecond">transactionsPerSecond</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          All the transactions per second results of the pgbench benchmark
<br/>
        </td>
      </tr>
      <tr>
        <td><b>transactionsProcessed</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The number of transactions processed.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.latency
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbench)</sup></sup>

The latency results of the pgbench benchmark


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchlatencyaverage">average</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Average latency of transactions
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchlatencystandarddeviation">standardDeviation</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The latency standard deviation of transactions.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.latency.average
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchlatency)</sup></sup>

Average latency of transactions


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The latency measure unit
<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          The latency average value
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.latency.standardDeviation
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchlatency)</sup></sup>

The latency standard deviation of transactions.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The latency measure unit
<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          The latency standard deviation value
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.statements[index]
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbench)</sup></sup>

Average per-statement latency (execution time from the perspective of the client) of a command after the benchmark finishes

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>command</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The command<br/>
        </td>
      </tr>
      <tr>
        <td><b>latency</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          Average latency of the command<br/>
        </td>
      </tr>
      <tr>
        <td><b>script</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The script index (`0` if no custom scripts have been defined)<br/>
        </td>
      </tr>
      <tr>
        <td><b>unit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The average latency measure unit<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbench)</sup></sup>

All the transactions per second results of the pgbench benchmark


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchtransactionspersecondexcludingconnectionsestablishing">excludingConnectionsEstablishing</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Number of Transactions Per Second (tps) excluding connection establishing.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchtransactionspersecondincludingconnectionsestablishing">includingConnectionsEstablishing</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Number of Transactions Per Second (tps) including connection establishing.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarkpgbenchtransactionspersecondovertime">overTime</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          The Transactions Per Second (tps) values aggregated over unit of time<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchtransactionspersecond)</sup></sup>

Number of Transactions Per Second (tps) excluding connection establishing.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Transactions Per Second (tps) measure unit
<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          The Transactions Per Second (tps) excluding connections establishing value
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchtransactionspersecond)</sup></sup>

Number of Transactions Per Second (tps) including connection establishing.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>unit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Transactions Per Second (tps) measure unit
<br/>
        </td>
      </tr>
      <tr>
        <td><b>value</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          The Transactions Per Second (tps) including connections establishing value
<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.pgbench.transactionsPerSecond.overTime
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarkpgbenchtransactionspersecond)</sup></sup>

The Transactions Per Second (tps) values aggregated over unit of time

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>intervalDuration</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>number</td>
        <td>
          The interval duration used to aggregate the transactions per second.<br/>
        </td>
      </tr>
      <tr>
        <td><b>intervalDurationUnit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The interval duration measure unit<br/>
        </td>
      </tr>
      <tr>
        <td><b>values</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]number</td>
        <td>
          The Transactions Per Second (tps) values aggregated over unit of time<br/>
        </td>
      </tr>
      <tr>
        <td><b>valuesUnit</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The Transactions Per Second (tps) measures unit<br/>
        </td>
      </tr></tbody>
</table>

#### SGDbOps.status.benchmark.sampling
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmark)</sup></sup>

The results of the sampling benchmark

<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarksamplingqueriesindex">queries</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The queries sampled.<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgdbopsstatusbenchmarksamplingtopqueriesindex">topQueries</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          The top queries sampled with the stats from pg_stat_statements. If is omitted if `omitTopQueriesInStatus` is set to `true`.<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.sampling.queries[index]
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarksampling)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The query id of the representative statement calculated by Postgres<br/>
        </td>
      </tr>
      <tr>
        <td><b>query</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A sampled SQL query<br/>
        </td>
      </tr>
      <tr>
        <td><b>timestamp</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The sampled query timestamp<br/>
        </td>
      </tr></tbody>
</table>

##### SGDbOps.status.benchmark.sampling.topQueries[index]
<sup><sup>[↩ Parent](#sgdbopsstatusbenchmarksampling)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The query id of the representative statement calculated by Postgres<br/>
        </td>
      </tr>
      <tr>
        <td><b>stats</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>map[string]string</td>
        <td>
          stats collected by the top queries query<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.status.conditions[index]
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>lastTransitionTime</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Last time the condition transitioned from one status to another.<br/>
        </td>
      </tr>
      <tr>
        <td><b>message</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A human-readable message indicating details about the transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>reason</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The reason for the condition last transition.<br/>
        </td>
      </tr>
      <tr>
        <td><b>status</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Status of the condition, one of `True`, `False` or `Unknown`.<br/>
        </td>
      </tr>
      <tr>
        <td><b>type</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Type of deployment condition.<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.status.majorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

The results of a major version upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A failure message (when available)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances present when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingToRestartInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that are pending to be restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>phase</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The phase the operation is or was executing)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restartedInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that have been restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourcePostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres version currently used by the primary instance
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetPostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres version that the cluster will be upgraded to
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.status.minorVersionUpgrade
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

The results of a minor version upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A failure message (when available)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances present when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingToRestartInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that are pending to be restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restartedInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that have been restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>sourcePostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres version currently used by the primary instance
<br/>
        </td>
      </tr>
      <tr>
        <td><b>switchoverFinalized</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date indicating if and when the switchover finalized
<br/>
        </td>
      </tr>
      <tr>
        <td><b>switchoverInitiated</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date indicating if and when the switchover initiated
<br/>
        </td>
      </tr>
      <tr>
        <td><b>targetPostgresVersion</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The postgres version that the cluster will be upgraded (or downgraded) to
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.status.restart
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

The results of a restart


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A failure message (when available)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances present when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingToRestartInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that are pending to be restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restartedInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that have been restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>switchoverFinalized</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date indicating if and when the switchover finalized
<br/>
        </td>
      </tr>
      <tr>
        <td><b>switchoverInitiated</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date indicating if and when the switchover initiated
<br/>
        </td>
      </tr></tbody>
</table>

### SGDbOps.status.securityUpgrade
<sup><sup>[↩ Parent](#sgdbopsstatus)</sup></sup>

The results of a security upgrade


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>failure</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          A failure message (when available)
<br/>
        </td>
      </tr>
      <tr>
        <td><b>initialInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances present when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>pendingToRestartInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that are pending to be restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>primaryInstance</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The primary instance when the operation started
<br/>
        </td>
      </tr>
      <tr>
        <td><b>restartedInstances</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]string</td>
        <td>
          The instances that have been restarted
<br/>
        </td>
      </tr>
      <tr>
        <td><b>switchoverFinalized</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date indicating if and when the switchover finalized
<br/>
        </td>
      </tr>
      <tr>
        <td><b>switchoverInitiated</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          An ISO 8601 date indicating if and when the switchover initiated
<br/>
        </td>
      </tr></tbody>
</table>
