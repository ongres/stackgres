---
title: oke
weight: 3
url: install/prerequisites/backups/oke
description: Details about how to setup and configure the backups on OCI Object Storage.
showToc: true
---

## OCI Object Storage Setup

This section will illustrate how to configure backups on StackGres using OCI object Storage.
To do so, you will need to have the [OCI-CLI](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/cliconcepts.htm) installed to create the right permissions and the bucket on OCI Object Storage.

Create the right permissions and the user with following characteristics (that you may change):

* Bucket name: backup-demo-of-stackgres-io
* IAM User Group: stackgres-backup-group
* IAM Policy: stackfres-backup-policy
* IAM username: stackgres-demo-k8s-user
* Secret Credentials: oci-backup-bucket-secret

Create the stackgres-demo-k8s-user user:

```bash
  oci iam user create --name stackgres-demo-k8s-user --description 'Stackgres backup user'
```

Create the group that the user will be a part of, which will have access to the bucket for the backup:

```bash
  oci iam group create --name stackgres-backup-group --description 'Stackgres backup group'
```

Add the user to the group:

```bash
  oci iam group add-user \
  --group-id $( oci iam group list --name stackgres-backup-group --query data[0].id --raw-output) \
  --user-id $(oci iam user list --name stackgres-demo-k8s-user --query data[0].id --raw-output)
```

OCI Object Storage has API compability with AWS S3, but first you need to find out what compartment this compatibility is configured:

```bash
  export s3compartment_id=$(oci os ns get-metadata --query 'data."default-s3-compartment-id"' --raw-output)
```

Create the bucket inside the compartment that has the API compatibility.

```bash
  oci os bucket create \
  --compartment-id $s3compartment_id \
  --name backup-demo-of-stackgres-io
```

Create the policy to allow the recent created Group to use the Bucket Created:

```bash
  oci iam policy create \
  --compartment-id $s3compartment_id \
  --name stackfres-backup-policy \
  --description 'Polici to use Bucket for Stackgres backup' \
  --statements '["Allow group stackgres-backup-group to use bucket on compartment id '$s3compartment_id' where target.bucket.name = '/''backup-demo-of-stackgres-io'/''"]'
```

Now we need to create the access key to be used on backup creation. As output a file `access_key.json` will be generated:

```bash
  oci iam customer-secret-key create --display-name oci-backup-bucket-secret --user-id $(oci iam user list --name stackgres-demo-k8s-user --query data[0].id --raw-output) --raw-output | tee access_key.json
```

Use this script to generate the full endpoint that will be replaced on the sgobjectstorage-backupconfig1.yaml file bellow.

```bash
  echo 'https://'$(oci os ns get --query data --raw-output)'.compat.objectstorage.'$(oci iam region-subscription list | jq -r '.data[0]."region-name"')'.oraclecloud.com'
```

## Kubernetes Setup

To proceed, a Kubernetes `Secret` with the folling shape needs to be created:

```bash
kubectl create secret generic oke-backup-bucket-secret --from-literal="accessKeyId=<YOUR_ACCESS_KEY_HERE>"   --from-literal="secretAccessKey=<YOUR_SECRET_KEY_HERE>"

secret/oke-backup-bucket-secret created
```

Having the credentials secret created, we just need to create the object storage configuration and set the backup configuration.
 The object storage configuration it is governed by the CRD
 [SGObjectStorage]({{% relref "06-crd-reference/10-sgobjectstorage" %}}). This CRD allows to specify the object storage technology
 and parameters required and a reference to the above secret.

Create the file `sgobjectstorage-backupconfig1.yaml` replacing the endpoint and the region:

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

and deploy to Kubernetes:

```bash
kubectl apply -f sgobjectstorage-backupconfig1.yaml
```

The backup configuration can be set unser the section `.spec.configurations.backups` of the CRD
 [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}), among others, the retention window for the automated backups,
 when base backups are performed and performance parameters of the backup process.

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

Note that for this tutorial and demo purposes, backups are created every 5 minutes. Modify the
`.spec.backups[0].cronSchedule` parameter above to adjust to your own needs.

The above configuration will be applied when creating the SGCluster resource.

