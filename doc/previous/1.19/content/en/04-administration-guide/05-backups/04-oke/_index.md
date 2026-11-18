---
title: oke
weight: 3
url: /administration/backups/oke
aliases: [ /install/prerequisites/backups/oke ]
description: Details about how to set up and configure the backups on OCI Object Storage.
showToc: true
---

## OCI Object Storage Setup

This section shows how to configure backups on StackGres using OCI Object Storage.
You will need to have the [OCI-CLI](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/cliconcepts.htm) installed, to create the required permissions and the bucket on OCI Object Storage.

Create the required permissions and the user with following characteristics (that you may change):

* Bucket name: `my-stackgres-bucket`
* IAM User Group: `stackgres-backup-group`
* IAM Policy: `stackgres-backup-policy`
* IAM username: `stackgres-backup-user`
* Secret Credentials: `oci-backup-secret`

Create the `stackgres-backup-user` user:

```
oci iam user create --name stackgres-backup-user --description 'StackGres backup user'
```

Create the group that the user will be a part of, which will have access to the bucket:

```
oci iam group create --name stackgres-backup-group --description 'StackGres backup group'
```

Add the user to the group:

```
oci iam group add-user \
 --group-id $( oci iam group list --name stackgres-backup-group --query data[0].id --raw-output) \
 --user-id $(oci iam user list --name stackgres-backup-user --query data[0].id --raw-output)
```

OCI Object Storage is compatible with AWS S3.
You need to find out the S3 compartment ID:

```
export s3compartment_id=$(oci os ns get-metadata --query 'data."default-s3-compartment-id"' --raw-output)
```

Create the bucket inside the compartment that has S3 compatibility.

```
oci os bucket create \
 --compartment-id $s3compartment_id \
 --name my-stackgres-bucket
```

Create a policy to allow the created group to use the bucket:

```
  oci iam policy create \
  --compartment-id $s3compartment_id \
  --name stackgres-backup-policy \
  --description 'Policy to use the bucket for StackGres backups' \
  --statements '["Allow group stackgres-backup-group to use bucket on compartment id '$s3compartment_id' where target.bucket.name = '/''my-stackgres-bucket'/''"]'
```

Now we need to create the access key that is used for the backup creation.
The following creates a key and saves it to a file `access_key.json`:

```
oci iam customer-secret-key create \
 --display-name oci-backup-secret \
 --user-id $(oci iam user list --name stackgres-backup-user --query data[0].id --raw-output) \
 --raw-output \
 | tee access_key.json
```

Create the full endpoint URL that will be used in the `sgobjectstorage-backupconfig1.yaml` file below.

```
echo 'https://'$(oci os ns get --query data --raw-output)'.compat.objectstorage.'$(oci iam region-subscription list | jq -r '.data[0]."region-name"')'.oraclecloud.com'
```

## Kubernetes Setup

Create a Kubernetes secret with the following contents:

```
kubectl create secret generic oke-backup-secret \
 --from-literal="accessKeyId=<YOUR_ACCESS_KEY_HERE>" \
 --from-literal="secretAccessKey=<YOUR_SECRET_KEY_HERE>"
```

Having the credential secret created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration is governed by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.
This CRD allows you to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: objectstorage
spec:
  type: s3Compatible
  s3Compatible:
    bucket: my-stackgres-bucket
    endpoint: https://<Your-Tenancy-Namespace>.compat.objectstorage.<Your-OCI-Region>.oraclecloud.com
    region: <Your-OCI-Region>
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: oke-backup-secret
          key: accessKeyId
        secretAccessKey:
          name: oke-backup-secret
          key: secretAccessKey
```
