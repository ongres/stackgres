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





{{< crd-properties >}}
[
  {
    "description": "stackgres.io/v1",
    "immutable": true,
    "name": "apiVersion",
    "required": true,
    "type": "string"
  },
  {
    "description": "SGScript",
    "immutable": true,
    "name": "kind",
    "required": true,
    "type": "string"
  },
  {
    "description": "Refer to the Kubernetes API documentation for the fields of the `metadata` field.",
    "link": "https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta",
    "name": "metadata",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgscriptspec",
    "description": "",
    "name": "spec",
    "required": true,
    "type": "object"
  },
  {
    "anchor": "sgscriptstatus",
    "description": "",
    "name": "status",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

## SGScript.spec
<sup><sup>[↩ Parent](#sgscript)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "If `true`, when any script entry fails, it will not prevent subsequent script entries from being executed. `false` by default.",
    "name": "continueOnError",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "If `true` the versions will be managed by the operator automatically. The user will still be able to update them if needed. `true` by default.",
    "name": "managedVersions",
    "required": false,
    "type": "boolean"
  },
  {
    "anchor": "sgscriptspecscriptsindex",
    "description": "A list of SQL scripts.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

### SGScript.spec.scripts[index]
<sup><sup>[↩ Parent](#sgscriptspec)</sup></sup>

Scripts are executed in auto-commit mode with the user `postgres` in the specified database (or in database `postgres` if not specified).

Fields `script` and `scriptFrom` are mutually exclusive and only one of them is required.


{{< crd-properties >}}
[
  {
    "description": "Database where the script is executed. Defaults to the `postgres` database, if not specified.",
    "name": "database",
    "required": false,
    "type": "string"
  },
  {
    "default": -1,
    "description": "The id is immutable and must be unique across all the script entries. It is replaced by the operator and is used to identify the script for the whole life of the `SGScript` object.",
    "name": "id",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Name of the script. Must be unique across this SGScript.",
    "name": "name",
    "required": false,
    "type": "string"
  },
  {
    "description": "If not set or set to `false` the script entry will not be retried if it fails.\n\nWhen set to `true` the script execution will be retried with an exponential backoff of 5 minutes,\n  starting from 10 seconds and a standard deviation of 10 seconds.\n\nThis is `false` by default.",
    "name": "retryOnError",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.",
    "name": "script",
    "required": false,
    "type": "string"
  },
  {
    "anchor": "sgscriptspecscriptsindexscriptfrom",
    "description": "Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.\n\nFields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.",
    "name": "scriptFrom",
    "required": false,
    "type": "object"
  },
  {
    "description": "When set to `true` the script entry execution will include storing the status of the execution of this\n  script entry in the table `managed_sql.status` that will be created in the specified `database`. This\n  will avoid an operation that fails partially to be unrecoverable requiring the intervention from the user\n  if used in conjunction with `retryOnError`.\n\nIf set to `true` then `wrapInTransaction` field must be set.\n\nThis is `false` by default.",
    "name": "storeStatusInDatabase",
    "required": false,
    "type": "boolean"
  },
  {
    "description": "User that will execute the script. Defaults to the superuser username when not set (that by default is `postgres`).",
    "name": "user",
    "required": false,
    "type": "string"
  },
  {
    "description": "Version of the script. It will allow to identify if this script entry has been changed.",
    "name": "version",
    "required": false,
    "type": "integer"
  },
  {
    "description": "Wrap the script in a transaction using the specified transaction mode:\n\n* `read-committed`: The script will be wrapped in a transaction using [READ COMMITTED](https://www.postgresql.org/docs/current/transaction-iso.html#XACT-READ-COMMITTED) isolation level.\n* `repeatable-read`: The script will be wrapped in a transaction using [REPEATABLE READ](https://www.postgresql.org/docs/current/transaction-iso.html#XACT-REPEATABLE-READ) isolation level.\n* `serializable`: The script will be wrapped in a transaction using [SERIALIZABLE](https://www.postgresql.org/docs/current/transaction-iso.html#XACT-SERIALIZABLE) isolation level.\n\nIf not set the script entry will not be wrapped in a transaction",
    "name": "wrapInTransaction",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

#### SGScript.spec.scripts[index].scriptFrom
<sup><sup>[↩ Parent](#sgscriptspecscriptsindex)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


{{< crd-properties >}}
[
  {
    "anchor": "sgscriptspecscriptsindexscriptfromconfigmapkeyref",
    "description": "A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.",
    "name": "configMapKeyRef",
    "required": false,
    "type": "object"
  },
  {
    "anchor": "sgscriptspecscriptsindexscriptfromsecretkeyref",
    "description": "A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.",
    "name": "secretKeyRef",
    "required": false,
    "type": "object"
  }
]
{{< /crd-properties >}}

##### SGScript.spec.scripts[index].scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgscriptspecscriptsindexscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key name within the ConfigMap that contains the SQL script to execute.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "The name of the ConfigMap that contains the SQL script to execute.",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

##### SGScript.spec.scripts[index].scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgscriptspecscriptsindexscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.35/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


{{< crd-properties >}}
[
  {
    "description": "The key of the secret to select from. Must be a valid secret key.",
    "name": "key",
    "required": false,
    "type": "string"
  },
  {
    "description": "Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).",
    "name": "name",
    "required": false,
    "type": "string"
  }
]
{{< /crd-properties >}}

## SGScript.status
<sup><sup>[↩ Parent](#sgscript)</sup></sup>



{{< crd-properties >}}
[
  {
    "anchor": "sgscriptstatusscriptsindex",
    "description": "A list of script entry statuses where a script entry under `.spec.scripts` is identified by the `id` field.",
    "name": "scripts",
    "required": false,
    "type": "[]object"
  }
]
{{< /crd-properties >}}

### SGScript.status.scripts[index]
<sup><sup>[↩ Parent](#sgscriptstatus)</sup></sup>



{{< crd-properties >}}
[
  {
    "description": "The hash of a ConfigMap or Secret referenced with the associated script entry.",
    "name": "hash",
    "required": false,
    "type": "string"
  },
  {
    "description": "The id that identifies a script entry.",
    "name": "id",
    "required": false,
    "type": "integer"
  }
]
{{< /crd-properties >}}
