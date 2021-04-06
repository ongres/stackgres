---
title: Debug
weight: 6
url: developer/stackgres/debug
---

# Enable Kubernetes Client Logs

Kubernetes client logs uses [OkHttp](https://square.github.io/okhttp/) library to perform operations
 with the Kubernetes API. To enable logs and see what it is sent and what is received set the
 `JAVA_OPTS` environment variable with value `-Dquarkus.log.category.\"okhttp3.logging.HttpLoggingInterceptor\".level=TRACE`
 for the operator Deployment:

```shell
kubectl set env -n stackgres deployment/stackgres-operator JAVA_OPTS=-Dquarkus.log.category.\"okhttp3.logging.HttpLoggingInterceptor\".level=TRACE
```
