---
title: AWS S3
weight: 1
url: tutorial/prerequisites/object-storage/aws-s3
---

First let's create the IAM policy that would allow the appropriate level of access to the S3 bucket:

```bash
export S3_BACKUP_BUCKET=YOUR_BUCKET_NAME

read -d '' policy <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "s3:ListBucket", "s3:GetBucketLocation" ],
      "Resource": [ "arn:aws:s3:::${S3_BACKUP_BUCKET}" ]
    },
    {
      "Effect": "Allow",
      "Action": [ "s3:PutObject", "s3:GetObject", "s3:DeleteObject" ],
      "Resource": [ "arn:aws:s3:::${S3_BACKUP_BUCKET}/*" ]
    }
  ]
}
EOF
```

Let's then create an IAM user and attach the above policy:

```bash
export AWS_PROFILE=     # optional
export AWS_REGION=us-east-2
export S3_BACKUP_BUCKET_USER=stackgres-s3-user

aws iam create-user --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER

aws iam put-user-policy --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER \
	--policy-name ${S3_BACKUP_BUCKET_USER}-policy --policy-document $policy
```

Then let's create an access key, the credentials that will be used to access this bucket. The following command will
output them, consider redirecting the command below to a file or non-printable command if working on a non-private
environment:

```bash
aws --output json iam create-access-key --region $AWS_REGION --user-name $S3_BACKUP_BUCKET_USER
```

Finally, create the bucket:

```bash
aws s3 mb s3://$S3_BACKUP_BUCKET --region $AWS_REGION
```
