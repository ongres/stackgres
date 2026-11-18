---
title: Change Web Console's user credentials
weight: 1
url: /administration/adminui/change/password
description: Changing the UI password.
showToc: true
---

### Changing the Web Console's user credentials

You can use the commands below to change the username and the password of an existing Web Console user (requires [jq](https://jqlang.github.io/jq/) command):

```
USERNAME=admin
NEW_USERNAME=root
NEW_PASSWORD=password

# Patch the StackGres user Secret associated with the user

kubectl get secret -A -l api.stackgres.io/auth=user \
  --template '{{ range .items }}{{ printf "%s %s %s\n" .metadata.namespace .metadata.name (.data.k8sUsername | base64decode) }}{{ end }}' \
  | grep " $USERNAME$" \
  | while read NAMESPACE SECRET_NAME USERNAME
    do
      kubectl patch secret -n "$NAMESPACE" "$SECRET_NAME" --type merge \
        -p "data: { password: \"$(echo -n "${NEW_USERNAME}${NEW_PASSWORD}"| sha256sum | cut -d ' ' -f 1 )\" }"
    done

# Patch any existing ClusterRoleBinding associated with the user

kubectl get clusterrolebindings -o json \
  | jq -r --arg USERNAME "$USERNAME" --arg NEW_USERNAME "$NEW_USERNAME" \
    '.items[]
      | select(.subjects != null and (.subjects | any(.apiGroup == "rbac.authorization.k8s.io" and .kind == "User" and .name == $USERNAME)))
      | .metadata.name + " " + (
        .subjects 
          | map(
            if .apiGroup == "rbac.authorization.k8s.io" and .kind == "User" and .name == $USERNAME
            then .name = $NEW_USERNAME else . end)
          | tojson
        )' \
  | while read CLUSTERROLEBINDING_NAME SUBJECTS
    do
      kubectl patch clusterrolebindings "$CLUSTERROLEBINDING_NAME" \
        --type merge -p "subjects: $SUBJECTS"
    done

# Patch any existing RoleBinding associated with the user

kubectl get rolebindings -A -o json \
  | jq -r --arg USERNAME "$USERNAME" --arg NEW_USERNAME "$NEW_USERNAME" \
    '.items[]
      | select(.subjects != null and (.subjects | any(.apiGroup == "rbac.authorization.k8s.io" and .kind == "User" and .name == $USERNAME)))
      | .metadata.namespace + " " + .metadata.name + " " + (
        .subjects 
          | map(
            if .apiGroup == "rbac.authorization.k8s.io" and .kind == "User" and .name == $USERNAME
            then .name = $NEW_USERNAME else . end)
          | tojson
        )' \
  | while read NAMESPACE ROLEBINDING_NAME SUBJECTS
    do
      kubectl patch rolebindings -n "$NAMESPACE" "$CLUSTERROLEBINDING_NAME" \
        --type merge -p "subjects: $SUBJECTS"
    done
```
