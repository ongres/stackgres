---
title: Backup settings
weight: 5
---

Backup settings are stored in a custom resource of with the following names:

 - **kind**: StackGresBackupConfig
 - **listKind**: StackGresBackupConfigList
 - **plural**: sgbackupconfigs
 - **singular**: sgbackupconfig
 
Default settings are stored in the same namespaces of the stackgres operator,
 with the name `defaultbackupconfig`

Given a stackgres operator installed in the `stackgres` namespace we can see the backup default values with de command:

``` sh
kubectl get sgbackupconfig -n stackgres defaultbackupconfig -o yaml
```

If a backup configuration is not specified in the cluster settings, a new one will be created with the default values. 

The default name of backup CR is `defaultpgbouncer`


