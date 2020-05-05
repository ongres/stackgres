---
title: Connecting to the Postgres cluster
weight: 6
---

## DNS resolution

With every stackgres cluster that you deploy a bunch of services will be deployed as well.  To connect to the database  you only need to be aware of two services: the primary and the replica service. 

The primary service is used to connect to the master node and the replica service is used to access the replica nodes. 

This services will follow a convention that is based in the cluster name and the function of the service, so that, the name of our services will be:
 
 - `<cluster-name>-primary`
 - `<cluster-name>-replicas`

Both services will accept connections from ports `5432` and `5433`. 

Therefore, given a cluster with name "stackgres" in the namespace "demo", the master node will accessible through 
 the URL: `stackgres-primary.demo.svc:5432`.  Meanwhile, the replica node is accessible through the URL: `stackgres-replicas.demo.svc:5432`


## Getting the database's password

All stackgres cluster store it's passwords in a secret that are located in the namespace of the stackgres cluster, 
 by convention, has the same name of the cluster. 

When a stackgres cluster it's created, it creates 3 users: 
  
  - superuser
  - replication.
  - authenticator

The passwords for this users are randomly generated and stored in the stackgres cluster secret in a key=value fashion.  Being the key a string in the format `<user>-password` and the value it's the password itself. 

Assuming that we have a stackgres cluster named "stackgres" in the namespace "demo", we can get the users passwords with following commands:

 - superuser: 
   ``` sh
   kubectl get secrets -n demo stackgres -o jsonpath='{.data.superuser-password}' | base64 -d
   ```
 - replication: 
   ``` sh
   kubectl get secrets -n demo stackgres -o jsonpath='{.data.replication-password}' | base64 -d
   ```
 - authenticator: 
   ``` sh
   kubectl get secrets -n demo stackgres -o jsonpath='{.data.authenticator-password}' | base64 -d
   ```

Note: the superuser's password is the same as the postgres password

## Examples

For all the following examples we're going to assume that we have a stackgres   cluster named "stackgres" in the namespace "demo"

### PSQL

With a pod with psql running in the same kubernetes cluster than the stackgres cluster, we can connect to the primary node with the following command: 

``` sh
PGPASSWORD=1775-d517-4136-958 psql -h stackgres-primary.demo.svc -U postgres
```


