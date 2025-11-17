---
title: Cluster Passwords
weight: 1
url: /administration/cluster/connection/passwords
aliases: [/administration/passwords/ ]
description: Describes how to retrieve the generated database passwords.
showToc: true
---

When creating a cluster, StackGres randomly generates passwords, for the `postgres` superuser and others.
The passwords are stored in a secret (named as the cluster).

By default, a StackGres cluster initialization creates 3 users:

- `superuser`
- `replication`
- `authenticator`

The passwords are stored in that secret under the keys `<user>-password`.

Assuming that we have a StackGres cluster named `cluster`, we can get the actual usernames and passwords with the following command:

- **superuser / postgres:**

```
kubectl get secret cluster \
  --template '{{ range $k, $v := .data }}{{ printf "%s: %s\n" $k ($v | base64decode) }}{{ end }}' \
  | grep '\(-username\|-password\): '
```
