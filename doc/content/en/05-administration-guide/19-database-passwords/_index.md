---
title: Database passwords
weight: 19
url: administration/passwords
description: Describes how to get the auto-generated database passwords.
---

All passwords are stored by the StackGres Operator in a secret located in the same StackGres Cluster's namespace and by convention, using the same name.

By default, a Stackgres Cluster initialization creates 3 users:
  
  - `superuser`
  - `replication`
  - `authenticator`

The passwords for this users are randomly generated and stored in the stackgres cluster secret in a key=value fashion.  Being the key a string in the format `<user>-password` and the value it's the password itself. 

Assuming that we have a Stackgres cluster named `stackgres` in the namespace `demo`, we can get the users passwords with following commands:

 - **superuser:**

   ```
   kubectl get secrets -n demo stackgres -o jsonpath='{.data.superuser-password}' | base64 -d
   ```
   > **Note:** the superuser's password is the same as the postgres password

 - **replication:** 

   ```
   kubectl get secrets -n demo stackgres -o jsonpath='{.data.replication-password}' | base64 -d
   ```
 - **authenticator:**
   
   ```
   kubectl get secrets -n demo stackgres -o jsonpath='{.data.authenticator-password}' | base64 -d
   ```


