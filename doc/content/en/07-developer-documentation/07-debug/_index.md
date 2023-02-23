---
title: Debug
weight: 6
url: developer/stackgres/debug
description: Details about how to enable DEBUG messages in the operator pods.
showToc: true
---

# Log Levels

One of the best options to debug StackGres is by setting log levels.
Log levels affect the logs of all StackGres components.

Log levels are divided into categories where each belongs a category tree.
A tree level is defined by a `.` (point).
For example, the category `io.stackgres` is a sub category of `io`.
For each category we can set a log level such as `INFO`, `DEBUG`, or `TRACE` (see https://quarkus.io/guides/logging#logging-levels for a complete list).
Here is a list of categories for each StackGres component:

| Component                    | Category                                  |
|:-----------------------------|-------------------------------------------|
| all                          | io.stackgres                              |
| operator                     | io.stackgres.operator                     |
| restapi controller           | io.stackgres.apiweb                       |
| cluster controller           | io.stackgres.cluster-controller           |
| distributedlogs controller   | io.stackgres.distributedlogs-controller   |
| patroni                      | io.stackgres.patroni                      |
| wal-g                        | io.stackgres.wal-g                        |
| envoy                        | io.stackgres.envoy                        |
| fluent-bit                   | io.stackgres.fluent-bit                   |
| fluentd                      | io.stackgres.fluentd                      |
| prometheus-postgres-exporter | io.stackgres.prometheus-postgres-exporter |

To set a log level for a specific category you can use the following command on the operator deployment:

```bash
kubectl set env -n stackgres deployment/stackgres-operator "APP_OPTS=-Dquarkus.log.category.\"$CATEGORY\".level=$LOG_LEVEL"
```

To see the effects of a change for components in a running Postgres cluster, you will have to restart it.
The necessity to restart is indicated in the `PendingRestart` condition of the `SGCluster` resource.

If you need to a set log level in the REST API controller, use following command:

```bash
kubectl set env -n stackgres deployment/stackgres-restapi "APP_OPTS=-Dquarkus.log.category.\"$CATEGORY\".level=$LOG_LEVEL"
```

## Enable HTTP Logs

Sometimes it is helpful to debug the HTTP requests that are sent to/from the operator, REST API controller, or local controllers.

The operator, REST API controller, and local controllers are based on the Kubernetes client which uses the [OkHttp](https://square.github.io/okhttp/) library.
To enable HTTP logs to see what it is sent and received, set the category `okhttp3.logging.HttpLoggingInterceptor` to `TRACE`.
