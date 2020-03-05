---
title: Error Responses
weight: 1
---

The operator error responses follows the (RFC 7807)[https://datatracker.ietf.org/doc/rfc7807/?include_text=1]. 

That means that all of error messages follows the following structure: 

``` json
{
  "type": "https://stackgres.io/doc/<operator-version/07-operator-api/01-error-types/#<error-type>",
  "title": "The title of the error message",
  "detail": "A human readable description of what is the problem",
  "field": "If applicable the field that is causing the issue"  
}
```

# Error types

| Type | Summary | 
| ---- | ----------- |
| [postgres-blacklist](#postgres-blacklist) | The postgres configuration that is trying to be created or update contains blacklisted parameters |
| [postgres-major-version-mismatch](#postgres-major-version-mismatch) | The postgres configuration that you are using is targeted to a different major version that the one that your cluster has. |
| [invalid-custom-resource-reference](#invalid-custom-resource-reference) | The stackgres cluster you are trying to create or update holds a reference to a custom resource that don't exists  |
| [default-configuration](#default-configuration) | An attempt to update or delete a default configuration has been detected |
| [forbidden-custom-resource-deletion](#forbidden-custom-resource-deletion) | You are attempting to delete a custom resource that cluster depends on it |
| [forbidden-custom-resource-update](#forbidden-custom-resource-update) | You are attempting to update a custom resource that cluster depends on it |
| [forbidden-cluster-update](#forbidden-cluster-update) | You are trying to update a cluster property that should not be updated |
| [invalid-storage-class](#invalid-storage-class) | You are trying to create a cluster using a storage class that doesn't exists |
| [constraint-violation](#constraint-violation) | One of the properties of the CR that you are creating or updating violates its syntactic rules.

## Postgres Blacklist

Some postgres configuration properties are managed automatically by stackgres, therefore you cannot include them. 

The blacklisted configuration properties are:

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

## Invalid Custom Resource Reference

This error means that you are trying to create or update a stackgres cluster using a reference to a 
 custom resource that doesn't exists in the same namespace. 

For example: 

Supose that we are trying to create a stackgres cluster with the following YAML.

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 1
  pgVersion: '11.6'
  volumeSize: '5Gi'
  pgConfig: 'postgresconf'
```

In order to create the cluster successfully, a postgres configuration with the name "postgresconf"
 must exists in the default namespace.

The same principle applies for the properties: connectionPoolingConfig, resourceProfile, backupConfig.

## Default configuration

When the operator is first installed a set of default CRs are created in the namespace in which the 
 operator is installed. 

If you try to update or delete any of those CRs, you will get this error. 

## Forbidden Custom Resource Deletion

A stackgres cluster configuration is composed in several CR. When you create any of this CRs: 
 StackGresPostgresConfig, StackGresConnectionPoolingConfig, StackGresProfile or 
 StackGresBackupConfig.  You can delete them if you want, until you create a cluster that references 
 one of these CRs. 

Once a stackgres cluster references any of the above mentioned CRs those become protected against 
 deletion. 

Suppose that you do execute the followig command: 

``` bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
spec:
  pgVersion: "12"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
EOF
```

At this point, you can delete the created CR without any issue. 

Nonetheless, when you execute the following command:

``` bash
cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 1
  pgVersion: '12.1'
  volumeSize: '5Gi'
  pgConfig: 'postgresconf'
EOF
```

The postgresconf CR becomes protected against deletion, and if you try to delete it you will get an error of this type.


## Forbidden Custom Resource Update

Most of CRs that can be referenced by a stackgres cannot be updated after a stackgres cluster 
 reference them. This is because, in some cases a configuration chage will require a postgres reboot, in others just a reload. 

In future versions we expect to do these types of operation automatically, and planned. But, since we 
 are not there yet, the CRs: StackGresPostgresConfig, StackGresConnectionPoolingConfig and StackGresProfile becomes update protected once are referenced by a cluster. 

The exception to these rule is the StackGresBackupConfig CR, which can be updated at any time. 


## Forbidden Cluster Update

After a stackgres cluster is created some of it's properties cannot be updated. By a stackgres cluster 
 a mean a CR of kind StackGresCluster. 

These properties are: 

* pgVersion
* volumeSize
* pgConfig
* connectionPoolingConfig
* resourceProfile
* storageClass
* sidecars
* restore

If you try to update any of these properties, you will receive a error of this type. 


## Invalid Storage Class

If you specify a storage class in the cluster creation, that storage have to be already configured. 

If it doesn't you will get an error.

## Constraint Violations

All fields of all Stackgres CRs have some limitations regarding of the value type, maximum, minimum 
 values, etc. All these of limitations are described in the documentation of each CR. 

Any violation of these limitations will trigger an error of these. 

The details of the error should indicate which CR limitation are you violating, with the CR itself.

## Postgres Major Version Mismatch

When you create a stackgres cluster you have to specify postgres version do you want to use. Also you 
 can specify which postgres configuration do you want to use. 

Postgres configurations are targeted to a specific postgres major version. Therefore in order to use a 
 postgres configuration, cluster's postgres version and the postgres configuration's target version 
 should match. 

Supose that you have installed the a postgres configuration following:

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
spec:
  pgVersion: "12"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
```

Notice that the pgVersion property of the YAML above, says "12". This means that this configuration is
 targeted for postgresql versions 12.x. 

In order to use that postgres configuration, your stackgres cluster should have postgres version 12,
 like the following:

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 1
  pgVersion: '12.1'
  volumeSize: '5Gi'
  pgConfig: 'postgresconf'
```

Notice that the cluster pgVersion says 12.1. Therefore, you will be to install a cluster like the above.

Also if instead of using the yaml above, you try to create a cluster like the following (notice the pgVersion change):

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
spec:
  instances: 1
  pgVersion: '11.6'
  volumeSize: '5Gi'
  pgConfig: 'postgresconf'
```

This is because the cluster wants to use postgres 11 and your the postgres configuration is targeted for postgres 12.