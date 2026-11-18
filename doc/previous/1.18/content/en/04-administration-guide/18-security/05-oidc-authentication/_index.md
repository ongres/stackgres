---
title: OIDC Authentication
weight: 5
url: /administration/security/oidc-authentication
description: How to configure OpenID Connect (OIDC) authentication for the StackGres Web Console.
showToc: true
---

StackGres supports [OpenID Connect (OIDC)](https://openid.net/connect/) authentication for the Web Console. OIDC allows you to delegate authentication to an external identity provider (IdP) such as Keycloak, Okta, Azure AD, or Google, enabling single sign-on (SSO) and centralized user management.

## Prerequisites

- An OIDC-compatible identity provider configured with a client application
- The client ID and client secret from your identity provider
- The authorization server URL of your identity provider

## Configuration

OIDC authentication is configured through the [SGConfig]({{% relref "06-crd-reference/12-sgconfig" %}}) CRD under the `spec.authentication.oidc` section.

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  authentication:
    oidc:
      authServerUrl: https://idp.example.com/realms/stackgres
      clientId: stackgres-console
      credentialsSecret: my-client-secret
```

## Configuration Fields

| Field | Type | Description |
|-------|------|-------------|
| `authServerUrl` | string | The URL of the OIDC authorization server (e.g. `https://idp.example.com/realms/stackgres`). |
| `clientId` | string | The OIDC client ID registered with the identity provider. |
| `credentialsSecret` | string | The OIDC client secret as a plain string. |
| `tlsVerification` | string | TLS verification mode for communication with the IdP. One of `required`, `certificate-validation`, or `none`. |
| `clientIdSecretRef` | object | Reference to a Kubernetes Secret containing the client ID (fields: `name`, `key`). |
| `credentialsSecretSecretRef` | object | Reference to a Kubernetes Secret containing the client secret (fields: `name`, `key`). |

## TLS Verification

The `tlsVerification` field controls how StackGres verifies the identity provider's TLS certificate:

| Value | Description |
|-------|-------------|
| `required` | Full TLS verification including certificate and hostname validation (default). |
| `certificate-validation` | Validates the certificate chain but does not verify the hostname. |
| `none` | Disables TLS verification entirely. Not recommended for production. |

## Using Kubernetes Secrets

Instead of providing the client ID and credentials as plain strings, you can reference Kubernetes Secrets:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: oidc-credentials
  namespace: stackgres
type: Opaque
stringData:
  client-id: stackgres-console
  client-secret: my-client-secret
---
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  authentication:
    oidc:
      authServerUrl: https://idp.example.com/realms/stackgres
      tlsVerification: required
      clientIdSecretRef:
        name: oidc-credentials
        key: client-id
      credentialsSecretSecretRef:
        name: oidc-credentials
        key: client-secret
```

## Example: Keycloak Integration

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  authentication:
    oidc:
      authServerUrl: https://keycloak.example.com/realms/stackgres
      clientId: stackgres-console
      credentialsSecret: keycloak-client-secret
      tlsVerification: required
```

## Related Documentation

- [SGConfig CRD Reference]({{% relref "06-crd-reference/12-sgconfig" %}})
- [Security]({{% relref "04-administration-guide/18-security" %}})
