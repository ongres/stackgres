---
title: Object Storage
weight: 4
url: tutorial/prerequisites/object-storage
description: Details about the object storage set up for the tutorial.
---

StackGres stores backups on object storage buckets. Currently supported are S3, GCS, Azure Blob and S3-compatible APIs.
You will need a bucket and user credentials to access the bucket, create paths and read and write to it.

You should refer to the documentation of the respective providers on how to configure and provide appropriate
credentials for your preferred type of object storage. For convenience, sample commands are contained within this
section on how to create buckets and credentials for some of the above object storage providers.

Providers:

{{% children style="li" depth="2" description="true" %}}

> Alternatively, you may consider using [MinIO](https://min.io/), which works as an S3-compatible API for StackGres, and
allows to run object storage locally (useful for bare-metal instances).
