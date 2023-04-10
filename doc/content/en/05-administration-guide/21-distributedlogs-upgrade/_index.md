---
title: DistributedLogs Upgrade
weight: 21
url: install/distributed-logs-upgrade
description: Details about how to upgrade the database node of a distributed logs.
---

This procedure can be used to upgrade a `SGDistributedLogs` after upgrading the StackGres operator
 to a newer version.

```
kubectl get sgdistributedlogs.stackgres.io -A --template '
{{- range $item := .items }}
  {{- range $item.status.conditions }}
    {{- if eq .type "PendingRestart" }}
      {{- printf "%s.%s %s=%s\n" $item.metadata.namespace $item.metadata.name .type .status }}
    {{- end }}
  {{- end }}
{{- end }}'
```

**The upgrade procedure will generate a service disruption**.
The service disruption will end when Patroni elects the new primary instance.

The procedure includes some shell script snippet examples. In those snippet we assume the
 following environment variables are set with values of the StackGres cluster you want to restart:

```
NAMESPACE=default
DISTRIBUTED_LOGS_NAME=example
```

## 1. Change operator version annotation

Change the `SGDistributedLogs` to indicate to use the new operator version.

```
OPERATOR_VERSION="$(helm list -o json -n stackgres \
  | jq -r '.[]|select(.name == "stackgres-operator").app_version')"
kubectl annotate sgdistributedlogs.stackgres.io -n "$NAMESPACE" "$DISTRIBUTED_LOGS_NAME" \
  --overwrite "stackgres.io/operatorVersion=$OPERATOR_VERSION"
```

## 2. Delete the StatefulSet and wait for the primary to be recreated

```
kubectl delete sts -n "$NAMESPACE" "$DISTRIBUTED_LOGS_NAME" --wait=true
```

Wait until the new instance is created and operational, receiving traffic from the service.
This new primary has already been initialized with the new components.

```
kubectl wait --for=condition=Ready -n "$NAMESPACE" "pod/$DISTRIBUTED_LOGS_NAME-0"
```
