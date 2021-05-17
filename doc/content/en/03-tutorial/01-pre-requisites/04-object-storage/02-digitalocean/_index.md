---
title: DigitalOcean
weight: 1
url: tutorial/prerequisites/object-storage/digitalocean-spaces
---

Go the [API page](https://cloud.digitalocean.com/settings/api/tokens) and create a spaces key.

You will also need to have installed the [s3Cmd](https://s3tools.org/download)
installed. Once installed, configure it following the [instructions in the oficial docs](https://docs.digitalocean.com/products/spaces/resources/s3cmd/).

Create the bucket:

```bash
export DO_SPACES_BACKUP_BUCKET=stackgres-tutorial
s3cmd mb s3://${DO_SPACES_BACKUP_BUCKET}
```
