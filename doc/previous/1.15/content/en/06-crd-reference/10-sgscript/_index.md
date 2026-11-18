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

See also [SQL Scripts section]({{%  relref "04-administration-guide/06-sql-scripts" %}}).





<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr><tr>
      <td><b>apiVersion</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>stackgres.io/v1</td>
      </tr>
      <tr>
      <td><b>kind</b></td>
      <td>✓</td>
      <td></td>
      <td></td>
      <td>string</td>
      <td>SGScript</td>
      </tr>
      <tr>
      <td><b><a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.27/#objectmeta-v1-meta">metadata</a></b></td>
      <td>✓</td>
      <td>✓</td>
      <td></td>
      <td>object</td>
      <td>Refer to the Kubernetes API documentation for the fields of the `metadata` field.</td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptspec">spec</a></b></td>
        <td>✓</td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptstatus">status</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          <br/>
        </td>
      </tr></tbody>
</table>

## SGScript.spec
<sup><sup>[↩ Parent](#sgscript)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>continueOnError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If `true`, when any script entry fail will not prevent subsequent script entries from being executed. `false` by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>managedVersions</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If `true` the versions will be managed by the operator automatically. The user will still be able to update them if needed. `true` by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptspecscriptsindex">scripts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of SQL scripts.
<br/>
        </td>
      </tr></tbody>
</table>

### SGScript.spec.scripts[index]
<sup><sup>[↩ Parent](#sgscriptspec)</sup></sup>

Scripts are executed in auto-commit mode with the user `postgres` in the specified database (or in database `postgres` if not specified).

Fields `script` and `scriptFrom` are mutually exclusive and only one of them is required.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>database</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Database where the script is executed. Defaults to the `postgres` database, if not specified.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The id is immutable and must be unique across all the script entries. It is replaced by the operator and is used to identify the script for the whole life of the `SGScript` object.
<br/>
          <br/>
            <i>Default</i>: -1<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the script. Must be unique across this SGScript.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>retryOnError</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          If not set or set to `false` the script entry will not be retried if it fails.

When set to `true` the script execution will be retried with an exponential backoff of 5 minutes,
  starting from 10 seconds and a standard deviation of 10 seconds.

This is `false` by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>script</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Raw SQL script to execute. This field is mutually exclusive with `scriptFrom` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptspecscriptsindexscriptfrom">scriptFrom</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>storeStatusInDatabase</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>boolean</td>
        <td>
          When set to `true` the script entry execution will include storing the status of the execution of this
  script entry in the table `managed_sql.status` that will be created in the specified `database`. This
  will avoid an operation that fails partially to be unrecoverable requiring the intervention from the user
  if user in conjunction with `retryOnError`.

If set to `true` then `wrapInTransaction` field must be set.

This is `false` by default.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>user</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          User that will execute the script. Defaults to the superuser username when not set (that by default is `postgres`) user.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>version</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          Version of the script. It will allow to identify if this script entry has been changed.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>wrapInTransaction</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Wrap the script in a transaction using the specified transaction mode:

* `read-committed`: The script will be wrapped in a transaction using [READ COMMITTED](https://www.postgresql.org/docs/current/transaction-iso.html#XACT-READ-COMMITTED) isolation level.
* `repeatable-read`: The script will be wrapped in a transaction using [REPEATABLE READ](https://www.postgresql.org/docs/current/transaction-iso.html#XACT-REPEATABLE-READ) isolation level.
* `serializable`: The script will be wrapped in a transaction using [SERIALIZABLE](https://www.postgresql.org/docs/current/transaction-iso.html#XACT-SERIALIZABLE) isolation level.

If not set the script entry will not be wrapped in a transaction
<br/>
        </td>
      </tr></tbody>
</table>

#### SGScript.spec.scripts[index].scriptFrom
<sup><sup>[↩ Parent](#sgscriptspecscriptsindex)</sup></sup>

Reference to either a Kubernetes [Secret](https://kubernetes.io/docs/concepts/configuration/secret/) or a [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) that contains the SQL script to execute. This field is mutually exclusive with `script` field.

Fields `secretKeyRef` and `configMapKeyRef` are mutually exclusive, and one of them is required.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptspecscriptsindexscriptfromconfigmapkeyref">configMapKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptspecscriptsindexscriptfromsecretkeyref">secretKeyRef</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>object</td>
        <td>
          A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGScript.spec.scripts[index].scriptFrom.configMapKeyRef
<sup><sup>[↩ Parent](#sgscriptspecscriptsindexscriptfrom)</sup></sup>

A [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) reference that contains the SQL script to execute. This field is mutually exclusive with `secretKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key name within the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The name of the ConfigMap that contains the SQL script to execute.
<br/>
        </td>
      </tr></tbody>
</table>

##### SGScript.spec.scripts[index].scriptFrom.secretKeyRef
<sup><sup>[↩ Parent](#sgscriptspecscriptsindexscriptfrom)</sup></sup>

A Kubernetes [SecretKeySelector](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.29/#secretkeyselector-v1-core) that contains the SQL script to execute. This field is mutually exclusive with `configMapKeyRef` field.


<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>key</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The key of the secret to select from. Must be a valid secret key.<br/>
        </td>
      </tr>
      <tr>
        <td><b>name</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          Name of the referent. [More information](https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#names).<br/>
        </td>
      </tr></tbody>
</table>

## SGScript.status
<sup><sup>[↩ Parent](#sgscript)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b><a href="#sgscriptstatusscriptsindex">scripts</a></b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>[]object</td>
        <td>
          A list of script entry statuses where a script entry under `.spec.scripts` is identified by the `id` field.
<br/>
        </td>
      </tr></tbody>
</table>

### SGScript.status.scripts[index]
<sup><sup>[↩ Parent](#sgscriptstatus)</sup></sup>



<table>
    <thead>
        <tr>
            <th><div style="width:12rem">Property</div></th>
            <th><div style="width:5rem">Required</div></th>
            <th><div style="width:5rem">Updatable</div></th>
            <th><div style="width:5rem">May Require Restart</div></th>
            <th><div style="width:5rem">Type</div></th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody><tr style="display:none;">
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td></td>
        <td>

Workaround for hugo bug not rendering first table row
<br/>
        </td>
      </tr>
      <tr>
        <td><b>hash</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>string</td>
        <td>
          The hash of a ConfigMap or Secret referenced with the associated script entry.
<br/>
        </td>
      </tr>
      <tr>
        <td><b>id</b></td>
        <td></td>
        <td>✓</td>
        <td></td>
        <td>integer</td>
        <td>
          The id that identifies a script entry.
<br/>
        </td>
      </tr></tbody>
</table>
