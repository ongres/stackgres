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
  initClusterRole: cluster-admin
  operator:
    image:
      pullPolicy: IfNotPresent
  restapi:
    name: stackgres-restapi
    image:
      pullPolicy: IfNotPresent
  adminui:
    name: stackgres-adminui
    image:
      pullPolicy: IfNotPresent
    service:
      exposeHTTP: false
      type: ClusterIP
  jobs:
    name: stackgres-jobs
    image:
      pullPolicy: IfNotPresent
  authentication:
    type: jwt
    oidc: {}
    user: admin
    resetPassword: false
    secretRef:
      name: ""
  cert:
    autoapprove: true
    certManager:
      autoConfigure: false
      duration: 2160h
      encoding: PKCS1
      renewBefore: 360h
      size: 2048
    createForOperator: true
    createForWebApi: true
    crt: null
    jwtRsaKey: null
    jwtRsaPub: null
    key: null
    resetCerts: false
    webCrt: null
    webKey: null
  extensions:
    cache:
      enabled: false
      persistentVolume:
        size: 1Gi
      preloadedExtensions:
      - x86_64/linux/timescaledb-1\.7\.4-pg12
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

See also [StackGres Installation section]({{%  relref "04-administration-guide/01-stackgres-installation" %}}).

{{% include "generated/SGConfig.md" %}}
