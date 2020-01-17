# StackGresBackupConfig

The Profile CRD represent the backups configuration for the cluster.

___

**Kind:** StackGresBackupConfig

**listKind:** StackGresBackupConfigList

**plural:** sgbackupconfigs

**singular:** sgbackupconfig
___

### **Properties**


| Property | Type | Description |
|-----------|------|-------------|
| retention | integer  | Retains specified number of full backups. Default is 5  |
| fullSchedule  | string  | Specify when to perform full backups using cron syntax:<br><minute: 0 to 59, or *> <hour: 0 to 23, or * for any value. All times UTC> <day of the month: 1 to 31, or *> <month: 1 to 12, or *> <day of the week: 0 to 7 (0 and 7 both represent Sunday), or *>. <br>If not specified full backups will be performed each day at 05:00 UTC  |
| fullWindow  | integer  | Specify the time window in minutes where a full backup will start happening after the point in time specified by fullSchedule. If for some reason the system is not capable to start the full backup it will be skipped. If not specified the window will be of 1 hour  |
| compressionMethod  | string  | To configure compression method used for backups. Possible options are: lz4, lzma, brotli. Default method is lz4. LZ4 is the fastest method, but compression ratio is bad. LZMA is way much slower, however it compresses backups about 6 times better than LZ4. Brotli is a good trade-off between speed and compression ratio which is about 3 times better than LZ4  |
| networkRateLimit  | integer  | To configure disk read rate limit during uploads in bytes per second  |
| uploadDiskConcurrency  | integer  | To configure how many concurrency streams are reading disk during uploads. By default 1 stream  |
| [pgpConfiguration]({{< ref "_pgp_key.md" >}}) | string  | The OpenPGP configuration for encryption and decryption backups with the following properties: - key: PGP private key  |
| tarSizeThreshold  | integer  | To configure the size of one backup bundle (in bytes). Smaller size causes granularity and more optimal, faster recovering. It also increases the number of storage requests, so it can costs you much money. Default size is 1 GB (1 << 30 - 1 bytes)  |
| [storage](_storage_configuration.md)  | object  | Backup storage configuration  |

