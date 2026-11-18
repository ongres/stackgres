---
title: Managed SQL Scripts
weight: 4
url: /administration/sql-scripts
aliases: [ /tutorial/using-initialdata , /tutorial/using-initialdata/scripts , /tutorial/using-managed-sql , /tutorial/using-managed-sql/scripts ]
description: "Details about how manage state and data using SQL scripts"
showToc: true
---

In StackGres, you can define SQL scripts that are executed on the database instances.
The [SGScript]({{% relref "06-crd-reference/10-sgscript" %}}) custom resource is used to define
 these scripts that are referenced in an SGCluster.

There are multiple ways to define scripts, depending on whether you need to define them as custom resource,
 within a ConfigMap, or within a Secret.

## Scripts definitions

The SGScript custom resource can be used to define inline scripts.

This shows an example inline SQL script inside of an SGScript:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
 name: script
spec:
 scripts:
 - name: create-my-database
   script: |
    CREATE DATABASE my-database OWNER postgres;
```

The script is referenced via its name in the SGCluster definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
 name: cluster
spec:
 managedSql:
  scripts:
  - sgScript: script
```

> **NOTE:** For sensitive data like credentials, create the script inside a Secret and reference the
>  Secret in the SGScript definition, instead (see below).

### ConfigMaps

It's also possible to define SQL scripts inside of ConfigMaps.

This shows how to create a ConfigMap that contains the SQL script:

```
kubectl create configmap init-tables \
  --from-literal=create-init-tables.sql="CREATE TABLE company(id integer, name char(50));"
```

> **NOTE:** To load more complex or larger queries, you can
>  [create the ConfigMap directly from your sql files](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap/#create-configmaps-from-files).

The ConfigMap can be referenced in the SGScript definition as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: script
spec:
  scripts:
  - name: create-init-tables
    scriptFrom:
      configMapKeyRef:
        name: init-tables
        key: create-init-tables.sql
```

### Secrets

For sensitive data such as credentials, a Secret is the preferred way to define SQL scripts.

This shows how to create a Secret that contains the SQL script:

```
kubectl create secret generic database-user \
  --from-literal=create-user.sql="CREATE USER demo PASSWORD 'demo'"
```

You can reference the Secret in the SGScript definition as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: script
spec:
  scripts:
  - name: create-user
    scriptFrom:
      secretKeyRef:
        name: database-user
        key: create-user.sql
```

## Referencing an SGScript

You may reference an SGScript in an SGCluster as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
# [...]
  managedSql:
    scripts:
    - sgScript: script
```

## Default SGScript

The SGCluster creates a default SGScript that contains some SQL scripts required to initialize properly the
 Postgres instance. This script is created with the same name as the SGCluster plus the `-default` suffix.

After creating an SGCluster the default SGScript is created and referenced automatically so the following:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
# [...]
  managedSql: {}
```

Will create the below SGCluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
# [...]
  managedSql:
    continueOnSGScriptError: false
    scripts:
    - id: 0
      sgScript: cluster-default
```

## Script lifecycle

This section will explain the lifecycle of the SQL scripts defined in the SGScripts that are referenced in an
 SGCluster. The SGCluster Pod-local controller is the component responsible of the execution of those scripts
 and follow the logic that will be explained below.

Each script entry in the SGScript under the section `SGScript.spec.scripts` has some fields that allow to
 configure some of the script lifecycle aspects:

* The `id` field that identifies a script entry.

* The `version` field that allow to version a script entry.

* For other fields see the [SGScript CRD reference section]({{% relref "06-crd-reference/10-sgscript#sgscriptspecscriptsindex" %}}).

Each script entry in the SGCluster under the section `SGCluster.spec.managedSql.scripts` has some fields that
 allow to configure some of the script lifecycle aspects:

* The `id` field that identifies an SGScript entry.

* The `sgScript` field that references the SGScript of an SGScript entry.

## SGScript identifier

The SGScript identifier (`id`) is usually managed by the operator itself so a user does not need to specify it.
 But in case the server-side apply method is used to patch or create the SGCluster resource you will need to
 set an unique positive integer (greater than 0 for an SGCluster and greater than 10 for an SGShardedCluster)
 value for each SGScript entry.

The SGScript identifier is used internally to map an SGScript with the SGCluster status (see below) and to
 allow as user to move an SGScript reference in the `SGCluster.spec.managedSql.scripts` section without losing
 its status.

## Script identifier

The script identifier (`id`) is usually managed by the operator itself so a user does not need to specify it.
 But in case the server-side apply method is used to patch or create the SGScript resource you will need to
 set an unique positive integer value for each script entry.

The script identifier is used internally to map a SGScript's script entry with the SGCluster status (see below)
 and to allow a user to move an SGScript's script entry in the `SGScript.spec.scripts` section without losing
 its status.
 
## Script versioning

Whenever the version is changed to any other value and the script was already been executed then it will be
 executed again (even if the execution of the previous version failed).

By default the operator set this field by incrementing its value (starting from 1) based on the content of
 the script (even if the script is defined in a ConfigMap or a Secret). To disable this behavior and have the
 `version` field controlled by the user set the field `SGScript.spec.managedVersions` to `false`.

## Execution order

The groups of scripts in the referenced SGScripts are executed sequentially
 following the order of the array `SGCluster.spec.managedSql.scripts`. By default, the execution is stopped
 as soon as any of the script in the group of scripts of any SGScript fails. This behavior can be changed by
 changing the value of the field `SGCluster.spec.managedSql.continueOnSGScriptError`. When this field is set
 to `true` the failure of any script in the group of scripts of an SGScript does not block the group of
 scripts in the following SGScript from being executed.

The scripts entry in an SGScript are executed sequentially following the order of the array
 `SGScript.spec.managedSql.scripts`. By default, the execution is stopped as soon as any of the script fails.
 This behavior can be changed by changing the value of the field `SGScript.spec.continueOnError`. When this
 field is set to `true` the failure of any script does not block the following script from being executed.

### Script status

Each entry in the section `SGCluster.spec.managedSql.scripts` has a field `id` that allows to map the status of
 an SGScript in the SGCluster status under the section `SGCluster.status.managedSql.scripts`.

Each entry in the section `SGScript.spec.scripts` has a field `id` and a `version` field that allows to map
 the status of an SGScript's script entry in the SGCluster status under the section
 `SGCluster.status.managedSql.scripts[].scripts`.

Whenever a script fails a failure message and a failure code will be set in the status under the section
 `SGCluster.status.managedSql.scripts[].scripts` of the corresponding SGScript's script entry `id` and `value`
 and the corresponding SGScript `id`. Like in the following example:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  name: error-script
spec: 
  scripts:
  - name: error-script
    script: "CREATE USER test WITH THE PASSWORD 'test'"
```

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
# [...]
  managedSql:
    scripts:
    - id: 0
      sgScript: cluster-default
    - id: 1
      sgScript: error-script
status:
# [...]
  managedSql:
    scripts:
    - completedAt: "2024-12-17T12:49:18.174664454Z"
      id: 0
      scripts:
      - id: 0
        version: 0
      - id: 4
        version: 0
      startedAt: "2024-12-17T12:49:18.043439358Z"
      updatedAt: "2024-12-17T12:49:18.143757177Z"
    - failedAt: "2024-12-17T12:49:18.229657936Z"
      id: 1
      scripts:
      - failure: |-
          ERROR: unrecognized role option "the"
            Position: 23
        failureCode: "42601"
        id: 0
        intents: 1
        version: 0
      startedAt: "2024-12-17T12:49:18.201156997Z"
      updatedAt: "2024-12-17T12:49:18.201188706Z"
```

