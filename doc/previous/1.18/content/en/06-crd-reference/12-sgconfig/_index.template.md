---
title: SGConfig
weight: 12
url: /reference/crd/sgconfig
description: Details about SGConfig
showToc: true
---

___

**Kind:** SGConfig

**listKind:** SGConfigList

**plural:** sgconfigs

**singular:** sgconfig
___

StackGres Operator configuration is stored in `SGConfig` Custom Resource.

> **WARNING**: Creating more than one SGConfig is forbidden in order to avoid misbehaviours. The single SGConfig should be created automatically during installation.

**Example:**

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-operator
spec:
  # Default values copied from <project_dir>/helm-charts/stackgres-operator/values.yaml
  containerRegistry: quay.io
  imagePullPolicy: IfNotPresent
  operator:
    image:
      pullPolicy: IfNotPresent
  restapi:
    name: stackgres-restapi
    image:
      pullPolicy: IfNotPresent
  adminui:
    image:
      pullPolicy: IfNotPresent
    service:
      exposeHTTP: false
      type: ClusterIP
  authentication:
    type: jwt
    user: admin
  extensions:
    repositoryUrls:
    - https://extensions.stackgres.io/postgres/repository
  grafana:
    autoEmbed: false
    datasourceName: Prometheus
    password: prom-operator
    schema: http
    user: admin
  prometheus:
    allowAutobind: true
```

See also [StackGres Installation section]({{%  relref "04-administration-guide/01-installation" %}}).

{{% include "generated/SGConfig.md" %}}
