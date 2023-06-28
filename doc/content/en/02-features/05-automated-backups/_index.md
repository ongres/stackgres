---
title: Automated Backups
weight: 5
url: /features/automated-backups
description: Automated backups and backup lifecycle management
---

Backups are a critical part of a database, and are key to any Disaster Recovery strategy.
StackGres includes backups based on continuous archiving, which allows for zero data loss recovery and PITR (Point-in-Time Recovery) to restore a database into an arbitrary past point in time.

StackGres also provides automated lifecycle management of the backups.
The backups are always stored in the most durable media available today: cloud object storage like [Amazon's S3](https://aws.amazon.com/s3/), [Google Cloud Storage](https://cloud.google.com/products/storage), or [Azure Blob](https://azure.microsoft.com/en-us/services/storage/blobs/).
If you are running on prem, you can use [Minio](https://min.io/) or other S3-compatible software to store your backups.

You need to provide your bucket access information and credentials, configure the retention policy, and everything else is fully automated by StackGres.
You can also create manual backups via a simple YAML file at any time.

Have a look at the [Backups Guide]({{% relref "04-administration-guide/04-backups" %}}) to learn more about how to define and manage backups.
