---
title: RBAC Authorization Overview
weight: 2
url: /api/rbac
description: Details about how to set up RBAC autorization.
showToc: true
---

## Authentication

In Kubernetes, you must be authenticated (logged in) before your request can be authorized (granted permission to access).
The same applies to StackGres' web UI.

You can choose between two authentication mechanism:
the first one stores the web UI username and password in a Kubernetes secret.
The second mechanism, available since StackGres version `1.3.0`, is using an OpenID Connect (OIDC) Provider.

### Kubernetes Secret Mechanism
By default, StackGres is configured to use Kubernetes secrets containing the username and password to authenticate REST API requests.
You can create users that can log in on the web UI.

The data that contains the secret must be in Base64 format.
The password is stored by concatenating the username and the password and creating a SHA265 hash, similar to the following:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: webapi-user-demo
  namespace: stackgres
  labels:
    api.stackgres.io/auth: user
type: Opaque
data:
  apiUsername: "demo@example.com | b64enc"
  k8sUsername: "username | b64enc"
  password: "{{ user + password | sha256sum | b64enc }}"
```

You might wonder why there are two username fields in the secret.
The `apiUsername` is optional and is used to customize the name used for the login Web UI.
The `k8sUsername` is the username that is used to impersonate the API calls to K8s.

### OpenID Connect Provider Mechanism

If StackGres is configured to use the OpenID Connect (OIDC) authentication type, it will use the authorization code flow supported by OpenID Connect compliant authorization servers such as Keycloak.

StackGres allows to authenticate the users of the web UI by redirecting them to the OpenID Connect Provider (e.g.: Keycloak) where they login, and, once the authentication is complete, return to the web UI with the code confirming the successful authentication.

You can enable the OIDC auth type when installing StackGres using Helm, e.g.:

```
helm install --namespace stackgres stackgres-operator \
  --set-string authentication.type=oidc \
  --set-string authentication.oidc.authServerUrl=https://auth.example.com/realms/stackgres \
  --set-string authentication.oidc.clientId=web-api \
  --set-string authentication.oidc.credentialsSecret=kISXZuLum0z8304vQHzOfMNapYHPtLX4 \
  stackgres-charts/stackgres-operator
```

The `authentication.type` should be set to `oidc`, `authentication.oidc.authServerUrl` should point to your OpenID Connect
Provider, `authentication.oidc.clientId` and `authentication.oidc.credentialsSecret` should be your corresponding client ID
and secret used for authenticating StackGres against the OIDC provider.

If you need to map an OIDC username to a different username in Kubernetes (similar to the `k8sUsername` in the secret), your OIDC provider should return an additional claim named `stackgres_k8s_username`.
In this way, you can map a user attribute with the username that Kubernetes should use to validate the RBAC permissions.
The configuration can differ from one provider to another, so please check the documentation of the OIDC provider that you are using.

## Using RBAC Authorization

Role-based access control (RBAC) is a method of regulating access to computer or network resources based on the roles of individual users within your organization.

RBAC authorization uses the rbac.authorization.k8s.io API group to make authorization decisions, allowing you to dynamically configure policies through the Kubernetes API.

> Kubernetes supports other authorization modes like attribute-based access control (ABAC), but the RBAC mode must be enabled for this to work.

Kubernetes authorizes API requests using the API server.
It evaluates all of the request attributes against the policies and allows or denies the request.
All parts of an API request must be allowed by the policies in order to proceed.
This means that permissions are denied by default.

### API Objects

The RBAC API declares four kinds of Kubernetes object: _Role_, _ClusterRole_, _RoleBinding_ and _ClusterRoleBinding_.
You can describe objects, or amend them, using tools such as kubectl, just like any other Kubernetes object.

> Please check https://kubernetes.io/docs/reference/access-authn-authz/rbac/#api-overview for a comprehensive description of how it works.

### ClusterRole

An RBAC cluster role contains rules that represent a set of permissions.
Permissions are purely additive (there are no "deny" rules).

StackGres handles resources in different namespaces, so as of today, a cluster role is required for StackGres to work properly.

#### ClusterRole Example

The following example shows a cluster role with basic permissions to read StackGres resources:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: stackgres-reader
rules:
- apiGroups: [""]
  resources:
    - namespaces
    - pods
    - secrets
  verbs: ["get", "list"]
- apiGroups: ["storage.k8s.io"]
  resources:
    - storageclasses
  verbs: ["get", "list"]
- apiGroups: ["apiextensions.k8s.io"]
  resources:
    - customresourcedefinitions
  verbs: ["get", "list"]
- apiGroups: ["stackgres.io"]
  resources:
    - sgclusters
    - sgpgconfigs
    - sgbackupconfigs
    - sgbackups
    - sgdistributedlogs
    - sginstanceprofiles
    - sgpoolconfigs
    - sgobjectstorages
    - sgscripts
  verbs: ["get", "list"]
```

#### ClusterRoleBinding

A role binding grants the permissions defined in a role to a user or set of users.

The following example "binds" the previous `stackgres-reader` cluster role to the `example` user:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  creationTimestamp: "2020-07-15T16:36:22Z"
  name: sg-restapi-example-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: stackgres-reader
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: example
```

The same can be achieved with: `kubectl create clusterrolebinding sg-restapi-example-user --clusterrole=stackgres-reader --user=example`

> Please note that the `example` user must also be mapped in the secret with a password to be able to login to the web UI.

## Determine the Request Verb

The following is a table with the resources of StackGres that can be used for defining the `ClusterRole`:

| Resource                  | API Group            | Verbs                                         |
|---------------------------|----------------------|-----------------------------------------------|
| sgclusters                | stackgres.io         | get, list, create, update, patch, delete      |
| sgpgconfigs               | stackgres.io         | get, list, create, update, patch, delete      |
| sginstanceprofiles        | stackgres.io         | get, list, create, update, patch, delete      |
| sgbackups                 | stackgres.io         | get, list, create, update, patch, delete      |
| sgbackupconfigs           | stackgres.io         | get, list, create, update, patch, delete      |
| sgdistributedlogs         | stackgres.io         | get, list, create, update, patch, delete      |
| sgpoolconfigs             | stackgres.io         | get, list, create, update, patch, delete      |
| sgobjectstorages          | stackgres.io         | get, list, create, update, patch, delete      |
| sgscripts                 | stackgres.io         | get, list, create, update, patch, delete      |
| customresourcedefinitions | apiextensions.k8s.io | get, list                                     |
| namespaces                |                      | get, list                                     |
| pods                      |                      | get, list                                     |
| secrets                   |                      | get, list, create, update, patch              |
| configmaps                |                      | get, list, create, update, patch              |
| storageclasses            | storage.k8s.io       | get, list                                     |

This is not an exhaustive list, but it should help to get started.
