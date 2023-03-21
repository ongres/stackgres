---
title: SGInstanceProfile
weight: 2
url: reference/crd/sginstanceprofile
description: Details about SGInstanceProfile
---

The `SGInstanceProfile` custom resource represents the CPU and memory resources assigned to each pod of the Postgres cluster.

___
**Kind:** SGInstanceProfile

**listKind:** SGInstanceProfileList

**plural:** sginstanceprofiles

**singular:** sginstanceprofile
___

**Spec**

| <div style="width:7rem">Property</div> | Required | Updatable | <div style="width:6rem">Default</div> | <div style="width:4rem">Type</div> | Description |
|:---------------------------------------|----------|-----------|:--------------------------------------|:-----------------------------------|:------------|
| cpu                                    | ✓        | ✓         | 1                                     | string                             | {{< crd-field-description SGInstanceProfile.spec.cpu >}} |
| memory                                 | ✓        | ✓         | 2Gi                                   | string                             | {{< crd-field-description SGInstanceProfile.spec.memory >}} |
| [hugePages](#huge-pages)               |          | ✓         |                                       | object                             | {{< crd-field-description SGInstanceProfile.spec.hugePages >}} |
| [containers](#containers)              |          | ✓         | generated                             | object                             | {{< crd-field-description SGInstanceProfile.spec.containers >}} |
| [initContainers](#init-containers)     |          | ✓         | generated                             | object                             | {{< crd-field-description SGInstanceProfile.spec.initContainers >}} |
| [requests](#requests)                  |          | ✓         |                                       | object                             | {{< crd-field-description SGInstanceProfile.spec.requests >}} |

Example:

```yaml
apiVersion: stackgres.io/v1
  kind: SGInstanceProfile
  metadata:
    name: size-l
  spec:
    cpu: "4"
    memory: 8Gi
```

## Huge Pages

| Property                           | Required | Updatable | Default | Type   | Description |
|:-----------------------------------|----------|-----------|:--------|:-------|:------------|
| hugepages-2Mi                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.hugePages.hugepages-2Mi >}} |
| hugepages-1Gi                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.hugePages.hugepages-1Gi >}} |

## Containers

| Property                                | Required | Updatable | Default | Type   | Description |
|:----------------------------------------|----------|-----------|:--------|:-------|:------------|
| memory                                  |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.containers.cpu >}} |
| cpu                                     |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.containers.memory >}} |
| [hugePages](#huge-pages-for-containers) |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.containers.hugePages >}} |

### Huge Pages for Containers

| Property                           | Required | Updatable | Default | Type   | Description |
|:-----------------------------------|----------|-----------|:--------|:-------|:------------|
| hugepages-2Mi                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.containers.hugePages.hugepages-2Mi >}} |
| hugepages-1Gi                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.containers.hugePages.hugepages-1Gi >}} |

## Init Containers

| Property                                     | Required | Updatable | Default | Type   | Description |
|:---------------------------------------------|----------|-----------|:--------|:-------|:------------|
| memory                                       |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.initContainers.cpu >}} |
| cpu                                          |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.initContainers.memory >}} |
| [hugePages](#huge-pages-for-init-containers) |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.initContainers.hugePages >}} |

### Huge Pages for Init Containers

| Property                           | Required | Updatable | Default | Type   | Description |
|:-----------------------------------|----------|-----------|:--------|:-------|:------------|
| hugepages-2Mi                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.initContainers.hugePages.hugepages-2Mi >}} |
| hugepages-1Gi                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.initContainers.hugePages.hugepages-1Gi >}} |

### Requests

| Property                                    | Required | Updatable | Default | Type   | Description |
|:--------------------------------------------|----------|-----------|:--------|:-------|:------------|
| cpu                                         |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.requests.cpu >}} |
| memory                                      |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.requests.memory >}} |
| [containers](#requests-containers)          |          | ✓         |         | object | {{< crd-field-description SGInstanceProfile.spec.requests.containers >}} |
| [initContainers](#requests-init-containers) |          | ✓         |         | object | {{< crd-field-description SGInstanceProfile.spec.requests.initContainers >}} |

#### Requests Containers

| Property                                | Required | Updatable | Default | Type   | Description |
|:----------------------------------------|----------|-----------|:--------|:-------|:------------|
| memory                                  |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.requests.containers.cpu >}} |
| cpu                                     |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.requests.containers.memory >}} |

#### Requests Init Containers

| Property                                     | Required | Updatable | Default | Type   | Description |
|:---------------------------------------------|----------|-----------|:--------|:-------|:------------|
| memory                                       |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.requests.initContainers.cpu >}} |
| cpu                                          |          | ✓         |         | string | {{< crd-field-description SGInstanceProfile.spec.requests.initContainers.memory >}} |

