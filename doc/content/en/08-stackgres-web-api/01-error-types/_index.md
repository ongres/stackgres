---
title: Error Responses
weight: 1
url: /api/responses/error
aliases: 
  - /07-developer-documentation/01-error-types
description: Details about the Error Codes, PostgreSQL Blocklist settings, and forbidden actions.
showToc: true
---

The operator error responses follows the [RFC 7807 - Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/rfc7807/?include_text=1).

That means that all error messages follow this structure:

``` json
{
  "type": "https://StackGres.io/doc/<operator-version>/api/responses/#<error-type>",
  "title": "The title of the error message",
  "detail": "A human readable description of what the problem is",
  "field": "If applicable, the field that is causing the issue"
}
```

## Error types

| Type                                                                  | Summary                                                                                                                    |
| --------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| [postgres-blocklist](#postgres-blocklist)                             | The Postgres configuration contains blocklisted parameters                          |
| [postgres-major-version-mismatch](#postgres-major-version-mismatch)   | The Postgres configuration is targeted to a different major version than the current version of your cluster. |
| [invalid-configuration-reference](#invalid-configuration-reference)   | The StackGres cluster holds a reference to a resource that doesn't exist.          |
| [default-configuration](#default-configuration)                       | An attempt to update or delete a default configuration has been detected.                                                   |
| [forbidden-configuration-deletion](#forbidden-configuration-deletion) | A resource that the cluster depends on is attempted to be deleted.                                                  |
| [forbidden-configuration-update](#forbidden-configuration-update)     | A resource that the cluster depends on is attempted to be updated.                                                  |
| [forbidden-cluster-update](#forbidden-cluster-update)                 | A certain cluster property that must not be updated is attempted to be updated.                                                     |
| [invalid-storage-class](#invalid-storage-class)                       | The StackGres cluster refers to a storage class that doesn't exist.                                               |
| [constraint-violation](#constraint-violation)                         | One of the resource properties that is created or updated violates its syntactic rules.                            |
| [forbidden-authorization](#forbidden-authorization)                   | You don't have permission to access the Kubernetes resource based on the RBAC rules.                                   |
| [invalid-secret](#invalid-secret)                                     | The StackGres cluster refers to a secret that doesn't exist.                                                      |
| [extension-not-found](#extension-not-found)                           | Some of the default or configured extensions can not be found in extensions repository                                      |
| [already-exists](#already-exists)                                     | The resource already exists.                                                                                                |
| [postgres-parameter](#postgres-parameter)                             | The Postgres configuration contains invalid parameters.                                                                     |

## Postgres Blocklist

Some Postgres configuration properties are managed automatically by StackGres, therefore you cannot include them.

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

This error means that you are trying to create or update a StackGres cluster using a reference to a resource that doesn't exist in the same namespace.

For example:

Suppose that we are trying to create a StackGres cluster with the following JSON.

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

In order to create the cluster successfully, a Postgres configuration with the name `postgresconf` must exists in the same namespace.

The same principle applies for the properties: `sgPoolingConfig`, `sgInstanceProfile`, `sgBackupConfig`.

## Default Configuration

When the operator is first installed, a set of default configuration objects are created in the operator's namespace.

If you try to update or delete any of those configurations, you will get this error.

## Forbidden Configuration Deletion

A StackGres cluster configuration is composed in several configuration objects.
You can only delete any of these resources, if no cluster references them.
Once a StackGres cluster references any of the above mentioned resources, those become protected against deletion.

Suppose that you create a config by sending the following `POST` request body to `/stackgres/pgconfig`:

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

This will create a Postgres configuration object with the name `postgresconf`.
At this point, you can delete the created object again without any issue.

However, if you create a cluster referencing the config, for example by sending the following to path `/stackgres/cluster`:

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

... the `postgresconf` object becomes protected against deletion.
If you try to delete it, you will get an error of type `forbidden-configuration-deletion`, since StackGres prevents you from deleting Postgres configuration resources that are used by an existing cluster.


## Forbidden Configuration Update

Some StackGres resources (or parts thereof) cannot be updated.
This is because certain updates require special handling that is not possible or not supported at this time.

In future versions, we would like StackGres to handle all types of operations automatically, and planned.
But, since we are not there yet, most of the configuration objects cannot be updated.

## Forbidden Cluster Update

After a StackGres cluster has been created, some of its properties cannot be updated.

These properties are:

* `version`
* `size`
* `configurations`
* `storageClass`
* `pods`
* `restore`

If you try to update any of these properties, you will receive an error of type `forbidden-cluster-update`.

## Invalid Storage Class

If you specify a storage class in the cluster creation, that storage have to be already configured.

If it doesn't you will get an error.

## Constraint Violations

The fields of the StackGres resources have restrictions regarding of the value type, maximum, minimum values, etc.
All of these restrictions are described in the documentation of each custom resource definition.

Any violation of these restrictions will cause an error.

The details of the error indicate which restriction is violated.

## Postgres Major Version Mismatch

When creating a StackGres cluster, you have to specify the Postgres version.
Also, you can specify a custom Postgres configuration.

Postgres configurations are targeted to a specific Postgres major version.
Therefore, in order to use a Postgres configuration, the cluster's Postgres version and the Postgres configuration's target version needs to match.

Assuming you create a Postgres configuration, for example by sending a `POST` request with the following body to `/stackgres/pgconfig`:

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

Note that the `postgresVersion` property specifies `12`.
This means that this configuration is targeted for Postgres version `12.x`.

In order to use that Postgres configuration, your StackGres cluster must also have Postgres version `12`, like the following:

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

Notice that the cluster version specifies `12.1`.
Therefore, you will be able to create a cluster like this.

But, if you instead try to create a cluster like the following (notice the version change):

```json
{
  "metadata": {
    "name": "StackGres"
  },
  "spec": {
    "instances": 1,
    "postgres": {
      "version": "11.1"
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

... you will receive an error.
This is because the cluster is specified with Postgres `11` and the Postgres configuration `postgresconf` is targeted for Postgres `12`.

## Forbidden Authorization

Role-based access control (`RBAC`) is a method of regulating access to computer or network resources based on the roles of individual users within your organization.

The StackGres REST API uses the RBAC authorization from Kubernetes.
Therefore, you need to specify the correct role bindings or cluster role bindings with the correct set of permissions for your user subject's role or cluster role.

When you don't have permissions to view or update a corresponding resource, or the permissions aren't configured correctly, you'll receive an error.

## Invalid Secret

If you specify a secret in the cluster creation that doesn't exist, you will receive an error.

## Extension Not Found

If you specify a Postgres extension that cannot be found in any repository, you will receive an error.

## Already Exists

A resource of the same type and name as the one you're trying to create in the same namespace already exists.

## Postgres Parameter

There are some invalid parameters in the configuration for the corresponding version of PostgreSQL.

For example, assume the following configuration:

```yaml
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

This configuration has two subtle errors.

The first invalid parameter is `default_toast_compression`.
While the name and value (`lz4`) of the parameter would be correct for PostgreSQL `14`, this config targets PostgreSQL `13`.

The second issue is that `checkpoint_timeout` minimum value is `30s`.
Setting this value lower than that is not valid.

StackGres validates these kinds of issues using a small library that comes from https://postgresqlco.nf/.
