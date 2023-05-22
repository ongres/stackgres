---
title: Debug
weight: 6
url: developer/stackgres/debug
description: Details about how to enable DEBUG messages in the operator pods.
showToc: true
---

# Logs Levels

One of the best option to debug StackGres is by setting logs levels. Logs levels affect logs of all the StackGres
 components. Logs level are divided into categories where each belongs a category tree. A tree level is defined
 by a `.` (point) (For example, category `io.stackgres` is a sub category of `io`). For each category we can set
 a log level like `INFO`, `DEBUG` or `TRACE` (see https://quarkus.io/guides/logging#logging-levels for a complete
 list). Here is a list of category for each component of StackGres:

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

To set a log level for a category you can use the following command on the Operator deployment:

```
kubectl set env -n stackgres deployment/stackgres-operator "APP_OPTS=-Dquarkus.log.category.\"$CATEGORY\".level=$LOG_LEVEL"
```

To make the change effective for components of a running cluster you will have to restart it when the `PendingRestart`
 condition of the `SGCluster` CR becomes `true`.

If you need to set log level for a category of the REST API Controller then use following command:

```
kubectl set env -n stackgres deployment/stackgres-restapi "APP_OPTS=-Dquarkus.log.category.\"$CATEGORY\".level=$LOG_LEVEL"
```

## Enable Operator, REST API Controller and Local Controllers HTTP Logs

Sometimes it comes handy in order to debug complex behaviour to being able to see what are sending and receiving
 operator, REST API Controller and Local Controllers components to Kubernetes API. Operator, REST API Controller
 and Local Controllers are based on Kubernetes client that uses [OkHttp](https://square.github.io/okhttp/) library
 to perform operations with the Kubernetes API. To enable HTTP logs and see what it is sent and what is received set
 the category `okhttp3.logging.HttpLoggingInterceptor` to level `TRACE`.
