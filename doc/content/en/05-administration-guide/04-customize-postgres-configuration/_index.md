---
title: Customize Postgres Configuration
weight: 4
url: administration/custom/postgres/config
description: Details about how to customize the PostgreSQL configuration.
---

StackGres operator creates default configurations in the same namespace where the operator has
 been installed. Those configuration CRs are read-only and can only be modified by the operator
 itself. User can create modified version of those default configurations by creating one in the
 same namespace where the cluster will be created.
 
When configuration resource is created the operator will look for a default configuration in the
 same namespace as the operator. With the default configuration found it will merge fields in the
 spec section that are not present in the created configuration filling them with values from the
 default configuration.

If a StackGres cluster is created without specifying PostgreSQL configuration
 (`SGPostgresConfig`) or resource profile configuration (`sgInstanceProfile`) the operator
 will look for default configuration of those kinds in the same namespace as the cluster or will
 create one using the default configuration in the same namespace where the operator is installed.

Here is the list of default configuration name with his kind:

| Name                                                        | Kind              |
|:------------------------------------------------------------|:------------------|
| postgres-<major version>-generated-from-default-<timestamp> | SGPostgresConfig  |
| generated-from-default-<timestamp>                          | SGInstanceProfile |
| generated-from-default-<timestamp>                          | SGPoolingConfig   |
