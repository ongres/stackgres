---
title: oke
weight: 3
url: install/prerequisites/backups/oke
description: Details about how to set up and configure the backups on OCI Object Storage.
showToc: true
---

## OCI Object Storage Setup

This section shows how to configure backups on StackGres using OCI Object Storage.
You will need to have the [OCI-CLI](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/cliconcepts.htm) installed, to create the required permissions and the bucket on OCI Object Storage.

Create the required permissions and the user with following characteristics (that you may change):

* Bucket name: `backup-demo-of-stackgres-io`
* IAM User Group: `stackgres-backup-group`
* IAM Policy: `stackgres-backup-policy`
* IAM username: `stackgres-demo-k8s-user`
* Secret Credentials: `oci-backup-bucket-secret`

Create the `stackgres-demo-k8s-user` user:

```bash
oci iam user create --name stackgres-demo-k8s-user --description 'Stackgres backup user'
```

Create the group that the user will be a part of, which will have access to the bucket:

```bash
oci iam group create --name stackgres-backup-group --description 'Stackgres backup group'
```

Add the user to the group:

```bash
oci iam group add-user \
 --group-id $( oci iam group list --name stackgres-backup-group --query data[0].id --raw-output) \
 --user-id $(oci iam user list --name stackgres-demo-k8s-user --query data[0].id --raw-output)
```

OCI Object Storage is compatible with AWS S3.
You need to find out the S3 compartment ID:

```bash
export s3compartment_id=$(oci os ns get-metadata --query 'data."default-s3-compartment-id"' --raw-output)
```

Create the bucket inside the compartment that has S3 compatibility.

```bash
oci os bucket create \
 --compartment-id $s3compartment_id \
 --name backup-demo-of-stackgres-io
```

Create a policy to allow the created group to use the bucket:

```bash
  oci iam policy create \
  --compartment-id $s3compartment_id \
  --name stackfres-backup-policy \
  --description 'Policy to use the bucket for Stackgres backups' \
  --statements '["Allow group stackgres-backup-group to use bucket on compartment id '$s3compartment_id' where target.bucket.name = '/''backup-demo-of-stackgres-io'/''"]'
```

Now we need to create the access key that is used for the backup creation.
The following creates a key and saves it to a file `access_key.json`:

```bash
oci iam customer-secret-key create \
 --display-name oci-backup-bucket-secret \
 --user-id $(oci iam user list --name stackgres-demo-k8s-user --query data[0].id --raw-output) \
 --raw-output \
 | tee access_key.json
```

Create the full endpoint URL that will be used in the `sgobjectstorage-backupconfig1.yaml` file below.

```bash
echo 'https://'$(oci os ns get --query data --raw-output)'.compat.objectstorage.'$(oci iam region-subscription list | jq -r '.data[0]."region-name"')'.oraclecloud.com'
```

## Kubernetes Setup

Create a Kubernetes secret with the following contents:

```bash
kubectl create secret generic oke-backup-bucket-secret \
 --from-literal="accessKeyId=<YOUR_ACCESS_KEY_HERE>" \
 --from-literal="secretAccessKey=<YOUR_SECRET_KEY_HERE>"
```

Having the credential secret created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

Create the file `sgobjectstorage-backupconfig1.yaml`, with your endpoint and region:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: backup-config-stackgres-demo
spec:
  type: s3Compatible
  s3Compatible:
    bucket: backup-demo-of-stackgres-io
    endpoint: https://<Your-Tenancy-Namespace>.compat.objectstorage.<Your-OCI-Region>.oraclecloud.com
    region: <Your-OCI-Region>
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: oke-backup-bucket-secret
          key: accessKeyId
        secretAccessKey:
          name: oke-backup-bucket-secret
          key: secretAccessKey
```

and deploy it to Kubernetes:

```bash
kubectl apply -f sgobjectstorage-backupconfig1.yaml
```

The backup configuration can be set under the section `.spec.configurations.backups` of the [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}) CRD.
Here we define the retention window for the automated backups and when base backups are performed.
Additionally, you can define performance-related configuration of the backup process.

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
spec:
  configurations:
    backups:
    - sgObjectStorage: backupconfig1
      cronSchedule: '*/5 * * * *'
      retention: 6
```

For this tutorial, backups are created every 5 minutes.
Change the `.spec.backups[0].cronSchedule` parameter according to your own needs.

The above configuration will be applied when the SGCluster resource is created.
