---
title: AWS S3
weight: 2
url: /administration/backups/eks
aliases: [ /install/prerequisites/backups/eks ]
description: Details about how to set up and configure backups on AWS S3.
showToc: true
---

## AWS S3 Setup

This section shows how to configure backups on StackGres using AWS S3.
You will need to have the [AWS CLI](https://aws.amazon.com/cli) installed, to create the required permissions and the bucket on AWS S3.

Let's create the required permissions, the user and the bucket with the following characteristics (that you may change):

* Zone: `us-west-2`
* Bucket name: `my-stackgres-bucket`
* IAM username: `stackgres-backup-user`

```
read -d '' AWS_S3_POLICY <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "s3:ListBucket", "s3:GetBucketLocation" ],
      "Resource": [ "arn:aws:s3:::my-stackgres-bucket" ]
    },
    {
      "Effect": "Allow",
      "Action": [ "s3:PutObject", "s3:GetObject", "s3:DeleteObject" ],
      "Resource": [ "arn:aws:s3:::my-stackgres-bucket/*" ]
    }
  ]
}
EOF

aws iam create-user --region us-west-2 --user-name stackgres-backup-user

aws iam put-user-policy --region us-west-2 --user-name stackgres-backup-user \
	--policy-name stackgres-backup-user-policy --policy-document "$AWS_S3_POLICY"
```

Now, we need to create the access key that is used for the backup creation.
The following creates a key and saves it to a file `credentials.json`:

```
aws --output json iam create-access-key --region us-west-2 --user-name stackgres-backup-user > credentials.json
```

Finally, create the bucket (`mb` stands for 'make bucket'):

```
aws s3 mb s3://my-stackgres-bucket --region us-west-2
```

## Secret and SGObjectStorage

Create a Kubernetes secret with the contents of our credentials:

```
accessKeyId=$(jq -r '.AccessKey.AccessKeyId' credentials.json)
secretAccessKey=$(jq -r '.AccessKey.SecretAccessKey' credentials.json)

kubectl create secret generic s3-backup-secret \
  --from-literal="accessKeyId=$accessKeyId" \
  --from-literal="secretAccessKey=$secretAccessKey"
```

Having the credentials secret created, we now need to create the object storage configuration and to set the backup configuration.
The object storage configuration it is governed by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.
This CRD allows to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: objectstorage
spec:
  type: s3
  s3:
    bucket: my-stackgres-bucket
    awsCredentials:
      secretKeySelectors:
        accessKeyId:
          name: s3-backup-secret
          key: accessKeyId
        secretAccessKey:
          name: s3-backup-secret
          key: secretAccessKey
```
