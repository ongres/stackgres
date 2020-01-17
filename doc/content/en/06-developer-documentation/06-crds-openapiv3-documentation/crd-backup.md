# StackGres Backup

The Profile CRD represent the main params for backup configuration.

___

**Kind:** StackGresProfile

**listKind:** StackGresBackupList

**plural:** sgbackups

**singular:** sgbackup
___

### **General Properties**


| Property | Type | Description |
|-----------|------|-------------|
| cluster | string  | The name of the cluster where the backup will or has been taken  |
| isPermanent | boolean  | Indicate if this backup is permanent and should not be removed by retention process  |
|   |   |   |


### **Backup Status information**

| Property | Type | Description |
|-----------|------|-------------|
| phase  | string  | The phase of the backup  |
| pod  | string  | The name of pod assigned to this backup. |
| failureReason  | string  | If the phase is failed this field will contain a message with the failure reason  |
| backupConfig  | string  | The name of the backup configuration used to perform this backup  |
| name  | string  | The name of the backup  |
| time  | string  | The date of the backup  |
| walFileName  | string  | The WAL file name when backup was started  |
| startTime | string  | The start time of backup  |
| finishTime  | string  | The finish time of backup  |
| hostname  | string  | The hostname of instance where the backup is taken  |
| dataDir  | string  | The data directory where the backup is taken  |
| pgVersion  | string  | The PostgreSQL version of the server where backup is taken  |
| startLsn  | string  | The LSN of when backup started  |
| finishLsn  | string  | The LSN of when backup finished  |
| isPermanent  | boolean  | Indicate internally if this backup is permanent and should not be removed by retention process.  |
| systemIdentifier  | string  | The internal system identifier of this backup  |
| uncompressedSize  | integer  | The size in bytes of the uncompressed backup  |
| compressedSize  | integer  | The size in bytes of the compressed backup  |
| controlData  | object  | An object containing data from the output of pg_controldata on the backup  |
| tested  | boolean  | true if the backup has been tested  |

