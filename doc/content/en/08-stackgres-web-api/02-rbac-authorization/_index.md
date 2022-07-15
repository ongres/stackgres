---
title: RBAC Authorization Overview
weight: 2
url: api/rbac
description: Details about how to setup RBAC autorization.
showToc: true
---

## Authentication

In Kubernetes, you must be authenticated (logged in) before your request can be authorized (granted permission to
access). The same applies to the Web UI of StackGres, you can choose between two authentication mechanism,
the first one uses a Kubernetes `Secret` to handle the username and password used by the Web UI, and
the other mechanism, available since version 1.3.0 of StackGres, is using an OpenID Connect Provider.

### Local `Secret` mechanism
By default, StackGres is configured to use a local `Secret` containing the username and password to authenticate
into the REST API, you can create users to authenticate against a Kubernetes using a special `Secret` designed
for that purpose to log in on the Web UI.

The data that contains the secret must be in base64 format, and the password should be the concatenation of the api
username plus the password itself.

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

You might wonder why are two username fields in the secret, the `apiUsername` is optional and is used to "customize" the
username used for the login Web UI, the `k8sUsername` is the username that is used to impersonate the API calls to K8s.

### OpenID Connect Provider mechanism
If StackGres is configured to use the OpenID Connect (OIDC) authentication type it will use OpenID Connect Authorization
Code Flow supported by OpenID Connect compliant Authorization Servers such as Keycloak.

StackGres allows to easily authenticate the users of the Web UI by redirecting them to the OpenID Connect Provider
(e.g.: Keycloak) to login and, once the authentication is complete, return them back with the code confirming the successful
authentication.

You can enable the OIDC auth type when installing StackGres using Helm, eg.:

```bash
helm install --namespace stackgres stackgres-operator \
  --set-string authentication.type=oidc \
  --set-string authentication.oidc.authServerUrl=https://auth.example.com/realms/stackgres \
  --set-string authentication.oidc.clientId=web-api \
  --set-string authentication.oidc.credentialsSecret=kISXZuLum0z8304vQHzOfMNapYHPtLX4 \
  stackgres-charts/stackgres-operator
```

The `authentication.type` should be set to `oidc`, `authentication.oidc.authServerUrl` should point to your OpenID Connect
Provider, `authentication.oidc.clientId` and `authentication.oidc.credentialsSecret` should be your corresponding Client ID
and Secret used for authentication of StackGres with agains the OIDC Provider.

If you need to map a OIDC username to a different username in Kubernetes (like the `k8sUsername` in local `Secret`), your OIDC
provider should return an additional Claim named `stackgres_k8s_username`, this way you can map an user attribute with the
username that Kubernetes should use to validate the RBAC permisions, this can be different from one provider to another so
please check the documentation of the OIDC Privider you are using.

## Using RBAC Authorization

Role-based access control (RBAC) is a method of regulating access to computer or network resources based on the roles of
individual users within your organization.

RBAC authorization uses the rbac.authorization.k8s.io API group to drive authorization decisions, allowing you to
dynamically configure policies through the Kubernetes API.

> Kubernetes supports others authorizations modes like Attribute-based access control (ABAC), but the RBAC mode must be
enabled in Kubernetes for this to work.

Kubernetes authorizes API requests using the API server. It evaluates all of the request attributes against all policies
and allows or denies the request. All parts of an API request must be allowed by some policy in order to proceed. This
means that permissions are denied by default.

### API objects

The RBAC API declares four kinds of Kubernetes object: _Role_, _ClusterRole_, _RoleBinding_ and _ClusterRoleBinding_. You can
describe objects, or amend them, using tools such as kubectl, just like any other Kubernetes object.

> Please check https://kubernetes.io/docs/reference/access-authn-authz/rbac/#api-overview  for a comprenhensive description
on how it works.

### ClusterRole

An RBAC ClusterRole contains rules that represent a set of permissions. Permissions are purely additive (there are no "deny" rules).

StackGres handles different namespaces, so for the moment a ClusterRole is required to properly work.

#### ClusterRole example

The following example shows a `ClusterRole` with basic permisions for read stackgres resources:

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
  verbs: ["get", "list"]
```

#### ClusterRoleBinding

A role binding grants the permissions defined in a role to a user or set of users.

The following example "binds" the previous `stackgres-reader` ClusterRole to the `example` user:

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

> Please note that the `example` user must also be mapped in the secret with a password to be able to login to the Web UI.

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
| customresourcedefinitions | apiextensions.k8s.io | get, list                                     |
| namespaces                |                      | get, list                                     |
| pods                      |                      | get, list                                     |
| secrets                   |                      | get, list                                     |
| storageclasses            | storage.k8s.io       | get, list                                     |

This is not an exhaustive list, but it should help to get started.
