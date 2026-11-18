---
title: SGScript
weight: 10
url: /reference/crd/sgscript
description: Details about SGScript
showToc: true
---

___

**Kind:** SGScript

**listKind:** SGScriptList

**plural:** sgscripts

**singular:** sgscript

**shortNames** sgscr
___

The `SGScript` custom resource represents an ordered list of versioned SQL scripts.

**Example:**

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGScript
metadata:
  name: script
spec:
  managedVersions: true
  continueOnError: false
  scripts:
  - name: create-stackgres-user
    scriptFrom:
      secretKeyRef: # read the user from a Secret to maintain credentials in a safe place
        name: stackgres-secret-sqls-scripts
        key: create-stackgres-user.sql
  - name: create-stackgres-database
    script: |
      CREATE DATABASE stackgres WITH OWNER stackgres;
  - name: create-stackgres-schema
    database: stackgres
    scriptFrom:
      configMapKeyRef: # read long script from a ConfigMap to avoid have to much data in the helm releasea and the sgcluster CR
        name: stackgres-sqls-scripts
        key: create-stackgres-schema.sql
```

See also [SQL Scripts section]({{%  relref "04-administration-guide/15-sql-scripts" %}}).

{{% include "generated/SGScript.md" %}}
