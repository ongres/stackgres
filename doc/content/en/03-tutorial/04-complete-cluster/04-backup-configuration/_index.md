---
title: Backup configuration
weight: 4
url: tutorial/complete-cluster/backup-configuration
description: Details about how to create custom backup configurations.
---

StackGres takes and maintains backups of your clusters automatically, if configured to do so. You will need an object
storage backup, see [Object Storage Prerequisite]({{% relref "03-tutorial/01-pre-requisites/04-object-storage" %}}) if you don't
have one configured yet.

Backups are created with Postgres continous archiving (physical backups), which allow essentially zero or close to zero
data loss. Base backups are taken regularly, and every generated WAL file is pushed to the object storage to account for
data changes in between base backups.

To access the bucket, StackGres will expect you to have configured a Kubernetes `Secret` where the bucket credentials
are stored. The keys of the secret may have any name that you want, they will be later referenced by a
`SecretKeySelector` where you can specify the names of the keys that contain the credentials. Credentials may vary
depending on the object storage technology used, continue to the technology of your choice link below to configure the
credentials and the backup configuration:

{{% children style="li" depth="1" description="true" %}}