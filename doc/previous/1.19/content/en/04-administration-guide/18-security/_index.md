---
title: Security
weight: 18
url: /administration/security
description: Security best practices and configuration for StackGres clusters.
---

This section covers security configuration and best practices for StackGres PostgreSQL clusters.

## Security Overview

StackGres provides multiple layers of security:

| Layer | Feature | Default |
|-------|---------|---------|
| **Transport** | SSL/TLS encryption | Enabled (auto-generated certificates) |
| **Authentication** | Password-based auth | Enabled (auto-generated passwords) |
| **Authorization** | PostgreSQL roles | Standard PostgreSQL RBAC |
| **Network** | Kubernetes Services | Internal cluster access |
| **Secrets** | Kubernetes Secrets | Credentials stored in Secrets |
| **Backups** | Encryption at rest | Available (optional) |

## Topics

{{% children style="li" depth="1" description="true" %}}
