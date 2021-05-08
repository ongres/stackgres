---
title: Instance Profile
weight: 1
url: tutorial/complete-cluster/instance-profile
description: Details about how to create the instances profiles.
---

An Instance Profile is an abstraction over the resource characteristics of an instance (basically, as of today, CPU
"cores" and RAM). It is represented in StackGres with the CRD
[SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}). You can think of instance profiles as
"t-shirt sizes", a way to create named t-shirt sizes, that you will reference when you create your clusters. It is a way
to enforce best practices by using standardized instance sizes.

Create the following file: `sginstanceprofile-small.yaml`:

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: demo
  name: size-small
spec:
  cpu: "4"
  memory: "8Gi"
```

and deploy to Kubernetes:

```bash
kubectl apply -f sginstanceprofile-small.yaml
```

You may create other instance profiles with other sizes if you wish.
