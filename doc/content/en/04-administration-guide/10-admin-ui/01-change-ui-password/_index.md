---
title: Change password
weight: 1
url: /administration/adminui/change/password
description: Changing the UI password.
showToc: true
---

### Changing the UI password

You can use the command below to change the password:

```
NEW_USER=admin
NEW_PASSWORD=password
kubectl create secret generic -n stackgres stackgres-restapi-admin  --dry-run=client -o json \
  --from-literal=k8sUsername="$NEW_USER" \
  --from-literal=password="$(echo -n "${NEW_USER}${NEW_PASSWORD}"| sha256sum | awk '{ print $1 }' )" > password.patch

kubectl patch secret -n stackgres stackgres-restapi-admin -p "$(cat password.patch)" && rm password.patch
```

Remember to remove the generated password hint from the secret to avoid security flaws:

```
kubectl patch secret --namespace stackgres stackgres-restapi-admin --type json -p '[{"op":"remove","path":"/data/clearPassword"}]'
```


To clean up the resources created by the demo just run:

```
kubectl delete --ignore-not-found -f {{< download-url >}}/stackgres-operator-demo.yml
```
