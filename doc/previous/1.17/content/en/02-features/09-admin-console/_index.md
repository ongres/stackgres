---
title: Management Web Console
weight: 9
url: /features/admin-console
description: Fully-featured management web console
---

StackGres comes with a fully-featured web console that allows you to read any information and to perform any operation that you could also do via `kubectl` and the StackGres CRDs.

![StackGres Web Console](web-console.png)

This web console is targeted for internal use by DBAs and can be exposed via `LoadBalancer` or other Kubernetes routing mechanisms.

The web console can be accessed via default admin credentials, via Kubernetes RBAC for user authentication, or SSO integration.
It also comes with a REST API for further flexibility.

Have a look at the [Admin UI Guide]({{% relref "04-administration-guide/13-admin-ui" %}}) to learn more about how to access and use it.

> The web console supports both light and dark modes to optimize your user experience.