---
title: Error Responses
weight: 1
url: api/responses/error
aliases: 
  - /07-developer-documentation/01-error-types
description: Details about the Error Codes, PostgreSQL Blocklist settings, and forbidden actions.
showToc: true
---

The operator error responses follows the [RFC 7807](https://datatracker.ietf.org/doc/rfc7807/?include_text=1).

That means that all of error messages follows the following structure:

``` json
{
  "type": "https://StackGres.io/doc/<operator-version>/07-operator-api/01-error-types/#<error-type>",
  "title": "The title of the error message",
  "detail": "A human readable description of what is the problem",
  "field": "If applicable the field that is causing the issue"
}
```

## Error types

| Type | Summary |
| ---- | ----------- |
| [postgres-blocklist](#postgres-blocklist) | The postgres configuration that is trying to be created or update contains blocklisted parameters |
| [postgres-major-version-mismatch](#postgres-major-version-mismatch) | The postgres configuration that you are using is targeted to a different major version that the one that your cluster has. |
| [invalid-configuration-reference](#invalid-configuration-reference) | The StackGres cluster you are trying to create or update holds a reference to a custom resource that don't exists  |
| [default-configuration](#default-configuration) | An attempt to update or delete a default configuration has been detected |
| [forbidden-configuration-deletion](#forbidden-configuration-deletion) | You are attempting to delete a custom resource that cluster depends on it |
| [forbidden-configuration-update](#forbidden-configuration-update) | You are attempting to update a custom resource that cluster depends on it |
| [forbidden-cluster-update](#forbidden-cluster-update) | You are trying to update a cluster property that should not be updated |
| [invalid-storage-class](#invalid-storage-class) | You are trying to create a cluster using a storage class that doesn't exists |
| [constraint-violation](#constraint-violation) | One of the properties of the CR that you are creating or updating violates its syntactic rules. |
| [forbidden-authorization](#forbidden-authorization) | You don't have the permisions to access the Kubernetes resource based on the RBAC rules. |
| [invalid-secret](#invalid-secret) | You are trying to create a cluster using a secret that doesn't exists |
| [extension-not-found](#extension-not-found) | Any of the default or configured extensions can not be found in extensions repository |
| [already-exists](#already-exists) | The resource already exists |
| [postgres-parameter](#postgres-parameter) | The postgres configuration contains invalid parameters |

## Postgres Blocklist

Some postgres configuration properties are managed automatically by StackGres, therefore you cannot include them.

The blocklisted configuration properties are:

| Parameters            |
| --------------------- |
| listen_addresses      |
| port                  |
| cluster_name          |
| hot_standby           |
| fsync                 |
| full_page_writes      |
| log_destination       |
| logging_collector     |
| max_replication_slots |
| max_wal_senders       |
| wal_keep_segments     |
| wal_level             |
| wal_log_hints         |
| archive_mode          |
| archive_command       |

## Invalid Configuration Reference

This error means that you are trying to create or update a StackGres cluster using a reference to a
 custom resource that doesn't exists in the same namespace.

For example:

Supose that we are trying to create a StackGres cluster with the following json.

```json
{
  "metadata": {
    "name": "StackGres"
  },
  "spec": {
    "instances": 1,
    "postgres": {
      "version": "11.6"
    },
    "pods": {
      "persistentVolume": {
        "size": "5Gi",

      }
    },
    "configurations": {
      "sgPostgresConfig": "postgresconf"
    }
  }
}
```

In order to create the cluster successfully, a postgres configuration with the name "postgresconf"
 must exists in the same namespace of the cluster that is being created.

The same principle applies for the properties: sgPoolingConfig, sgInstanceProfile, sgBackupConfig.

## Default configuration

When the operator is first installed a set of default configurations objects that are created in the namespace in which the
 operator is installed.

If you try to update or delete any of those configuraions, you will get this error.

## Forbidden Configuration Deletion

A StackGres cluster configuration is composed in several configuration objects. When you create a
 postgres, connection pooling, resource profile or backup configuration, you can delete them
 if you loke to, until you create a cluster that references one of these objets.

Once a StackGres cluster references any of the above mentioned objects those become protected against
 deletion.

Suppose that you send a the following request:

```
uri: /stackgres/pgconfig
method: POST
payload:
```
```json
{
  "metadata": {
    "name": "postgresconf"
  },
  "spec": {
    "postgresVersion": "12",
    "postgresql.conf": "password_encryption: 'scram-sha-256'\nrandom_page_cost: '1.5'"
  }
}
```

This will create a postgres configuration object with the name postgresconf.
At this point, you can delete the created object without any issue.

Nonetheless, if you send the request to the path /stackgres/cluster:

```
uri: /stackgres/cluster
method: POST
payload:
```
```json
{
  "metadata": {
    "name": "StackGres"
  },
  "spec": {
    "instances": 1,
    "postgres": {
      "version": "12.1"
    },
    "pods": {
      "persistentVolume": {
        "size": "5Gi",

      }
    },
    "sgPostgresConfig": "postgresconf"
  }
}
```

The postgresconf object becomes protected against deletion, and if you try to delete it you will get an
 error of this type to prevent deletion of postgresql configuration used by an existing cluster.


## Forbidden Configuration Update

Whole or parts of some objects cannot be updated. This is because changing it would require some
 particular handling that is not possible or not supported at this time.

In future versions we expect to do these types of operation automatically, and planned. But, since we
 are not there yet, must of out configuration object cannot be updated.

## Forbidden Cluster Update

After a StackGres cluster is created some of it's properties cannot be updated.

These properties are:

* version
* size
* configurations
* storageClass
* pods
* restore

If you try to update any of these properties, you will receive an error of this type.

## Invalid Storage Class

If you specify a storage class in the cluster creation, that storage have to be already configured.

If it doesn't you will get an error.

## Constraint Violations

All fields of all StackGres objects have some limitations regarding of the value type, maximum, minimum
 values, etc. All these of limitations are described in the documentation of each object.

Any violation of these limitations will trigger an error of these.

The details of the error should indicate which configuration limitation are you violating.

## Postgres Major Version Mismatch

When you create a StackGres cluster you have to specify the postgres version do you want to use. Also you
 can specify which postgres configuration do you want to use.

Postgres configurations are targeted to a specific postgres major version. Therefore in order to use a
 postgres configuration, cluster's postgres version and the postgres configuration's target version
 should match.

Suppose that create a postgres configuration with the following request:

```
uri: /stackgres/pgconfig
method: POST
payload:
```
```json
{
  "metadata": {
    "name": "postgresconf"
  },
  "spec": {
    "postgresVersion": "12",
    "postgresql.conf": "password_encryption: 'scram-sha-256'\nrandom_page_cost: '1.5'"
  }
}
```

Notice that the postgresVersion property says "12". This means that this configuration is
 targeted for postgresql versions 12.x.

In order to use that postgres configuration, your StackGres cluster should have postgres version 12,
 like the following:

```json
{
  "metadata": {
    "name": "StackGres"
  },
  "spec": {
    "instances": 1,
    "postgres": {
      "version": "12.1"
    },
    "pods": {
      "persistentVolume": {
        "size": "5Gi"
      }
    },
    "sgPostgresConfig": "postgresconf"
  }
}
```

Notice that the cluster version says 12.1. Therefore, you will be able to install a cluster like the above.

Also if instead of using the above payload, you try to create a cluster with the following request
 (notice the version change):
```
uri: /stackgres/cluster
method: POST
payload:
```
```json
{
  "metadata": {
    "name": "StackGres"
  },
  "spec": {
    "instances": 1,
    "postgres": {
      "version": "12.1"
    },
    "pods": {
      "persistentVolume": {
        "size": "5Gi"
      }
    },
    "sgPostgresConfig": "postgresconf"
  }
}
```

You will receive an error. This is because the cluster wants to use postgres 11 and your the postgres
 configuration is targeted for postgres 12.

## Forbidden Authorization

Role-based access control (`RBAC`) is a method of regulating access to computer or network resources
 based on the roles of individual users within your organization.

The REST API uses the RBAC Authorization from Kubernetes, so you should define correctly the subject `User`
 in the `RoleBinding` or `ClusterRoleBinding` with the correct set of permisions in a `Role` or `ClusterRole`

This error means that you either don't have access to the corresponding resource or that your permisions
 are not set correctly.

## Invalid Secret

If you specify a secret in the cluster creation, that secret have to already exsits.

If it doesn't you will get an error.

## Extension not found

If you specify an extension than can not be found in any repository you will get an error.

# Already Exists

A resource of same type in the same namespace and with the same name as the one you are trying to create already exists.

# Postgres Parameter

There are some invalid parameters in the configuration for the corresponding version of PostgreSQL.

For example, assume the following configuration:

```yml
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: postgresconf
spec:
  postgresVersion: "13"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    default_toast_compression: 'lz4'
    shared_buffers: '256MB'
    checkpoint_timeout: '15s'
```

This configuration has two subtle invalid parameters, the first one is `default_toast_compression`, while the name and
 value (`lz4`) of the parameter is correct for PostgreSQL 14, this config targets PostgreSQL 13.
 The second issue is that `checkpoint_timeout` min value is `30s`, setting this value lower creates an invalid setting.

StackGres validates this kind of issues using a small library that comes from https://postgresqlco.nf/.
