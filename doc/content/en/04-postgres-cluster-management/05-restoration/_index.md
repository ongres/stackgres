---
title: Restoration
weight: 5
---

The restoration configuration CR stores configuration parameters needed to
 perform a restoration

 ___
**Kind:** StackgresRestoreConfig

**listKind:** StackgresRestoreConfigList

**plural:** sgrestoreconfigs

**singular:** sgrestoreconfig
___

**Spec**

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| source | ✓ | object  | restoration source configuration. For detail see the restoration source [section](##restore-source).  | |
| compressionMethod |  | string  | Restoration compression method, could be:  lz4, lzma or brotli  | lz4 |
| downloadDiskConcurrency |  | integer | How many concurrent downloads will attempts during the restoration   | 1
| pgpConfiguration | | object | The OpenPGP configuration for encryption and decryption backups with the following properties: - key: PGP private key |

Example: 

``` yaml
apiVersion: stackgres.io/v1alpha1
kind: StackgresRestoreConfig
metadata:
  name: restoreconf
spec:
  source:
    fromBackup: 70f915a5-11ab-485a-a991-3ecfb7bbb8f0
  compressionMethod: lz4  
  downloadDiskConcurrency: 1
```

The restore default settings are stored in the same namespaces of the stackgres operator,
 with the name `defaultrestoreconfig`

Therefore, iven a stackgres operator installed in the `stackgres` namespace we can see the backup default values with de command:

``` sh
kubectl get sgrestoreconfigs.stackgres.io -n stackgres defaultrestoreconfig
```

## Restore source

| Property | Required | Type | Description | Default |
|-----------|------|------|-------------|------|
| fromBackup | | string  | The UID of the backup CR to restore. If configured, it will ignore the fromStorage and backupName options. | |
| autoCopySecrets |  | boolean  | If the backup is in another namespace, the secrets that holds the storage might be needed to copy in the new cluster namespaces. If is set to true, it will copy the required secrets  | false |
| fromStorage |  | object | Storage configuration in where is located the backup. If set, the backupName is required   | 
| backupName | | string | Name of the backup to restore. If 'LATEST' is used, it will restore the most recent backup found in the storage | LATEST


