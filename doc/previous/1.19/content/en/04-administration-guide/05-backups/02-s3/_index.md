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
The object storage configuration is governed by the [SGObjectStorage]({{% relref "06-crd-reference/09-sgobjectstorage" %}}) CRD.
This CRD allows you to specify the object storage technology, required parameters, as well as a reference to the credentials secret.

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

## Using IAM Roles for Service Accounts (IRSA)

For enhanced security on Amazon EKS, you can use [IAM Roles for Service Accounts (IRSA)](https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html) instead of static access keys. This eliminates the need to manage and rotate access keys.

### Prerequisites

- Amazon EKS cluster with OIDC provider configured
- AWS CLI and eksctl installed

### Step 1: Create the IAM Policy

Create a policy that grants access to your S3 bucket:

```bash
cat > s3-backup-policy.json <<EOF
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

aws iam create-policy \
  --policy-name StackGresBackupPolicy \
  --policy-document file://s3-backup-policy.json
```

### Step 2: Create IAM Role with Trust Policy

Using eksctl (recommended):

```bash
eksctl create iamserviceaccount \
  --name stackgres-backup-sa \
  --namespace default \
  --cluster my-eks-cluster \
  --attach-policy-arn arn:aws:iam::ACCOUNT_ID:policy/StackGresBackupPolicy \
  --approve
```

Or manually create the role with a trust policy:

```bash
# Get OIDC provider
OIDC_PROVIDER=$(aws eks describe-cluster --name my-eks-cluster \
  --query "cluster.identity.oidc.issuer" --output text | sed 's|https://||')

# Create trust policy
cat > trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/${OIDC_PROVIDER}"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${OIDC_PROVIDER}:sub": "system:serviceaccount:default:stackgres-backup-sa"
        }
      }
    }
  ]
}
EOF

aws iam create-role \
  --role-name StackGresBackupRole \
  --assume-role-policy-document file://trust-policy.json

aws iam attach-role-policy \
  --role-name StackGresBackupRole \
  --policy-arn arn:aws:iam::ACCOUNT_ID:policy/StackGresBackupPolicy
```

### Step 3: Configure SGObjectStorage with IAM Role

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: s3-iam-storage
spec:
  type: s3
  s3:
    bucket: my-stackgres-bucket
    region: us-west-2
    awsCredentials:
      useIAMRole: true
```

### Step 4: Configure SGCluster to Use the Service Account

Annotate the cluster's service account or configure it in the SGCluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  metadata:
    annotations:
      clusterPods:
        # This annotation is added to pods
        eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT_ID:role/StackGresBackupRole
  configurations:
    backups:
      - sgObjectStorage: s3-iam-storage
        cronSchedule: '0 5 * * *'
        retention: 7
```

### Benefits of IRSA

- **No static credentials**: Eliminates the need to store and rotate access keys
- **Fine-grained permissions**: Each cluster can have its own IAM role
- **Audit trail**: AWS CloudTrail logs which pods access S3
- **Automatic credential rotation**: AWS handles credential lifecycle

## S3 Storage Classes

You can specify the S3 storage class for cost optimization:

```yaml
apiVersion: stackgres.io/v1beta1
kind: SGObjectStorage
metadata:
  name: s3-infrequent-access
spec:
  type: s3
  s3:
    bucket: my-stackgres-bucket
    region: us-west-2
    storageClass: STANDARD_IA
    awsCredentials:
      useIAMRole: true
```

Available storage classes:

| Class | Description | Use Case |
|-------|-------------|----------|
| `STANDARD` | Default, high availability | Frequently accessed backups |
| `STANDARD_IA` | Infrequent Access | Backups accessed less than monthly |
| `REDUCED_REDUNDANCY` | Lower durability | Non-critical, reproducible data |

## Specifying Region

If your bucket is in a specific region, specify it to avoid additional API calls:

```yaml
spec:
  s3:
    bucket: my-stackgres-bucket
    region: us-west-2
```
