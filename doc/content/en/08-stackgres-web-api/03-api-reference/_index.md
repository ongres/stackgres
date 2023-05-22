---
title: StackGres REST API Reference
weight: 3
layout: swagger
url: api/reference
aliases: [ /08-stackgres-web-api/03-api-reference/ ]
description: Details about StackGres' REST API and its endpoints.
---

The StackGres REST API offers a way to interact with StackGres in a similar way as using the Kubernetes API through the StackGres CRDs, but with more functionality.
For example, multiple resources such as secrets or configmaps can be created in a single requests.
The REST API leverages [Kubernetes' RBAC permission system]({{% relref "/08-stackgres-web-api/02-rbac-authorization" %}}).

The API is provided by a Kubernetes deployment that is installed together with the operator.
The API is mainly used by the web UI that runs in the same application.

Swagger is in essence an Interface Description Language for describing RESTful APIs using JSON.
Swagger is used together with a set of open-source software tools to design, build, document, and use RESTful web services.
Swagger includes automated documentation, code generation (into many programming languages), and test-case generation.

To access those endpoints that require authentication, you will need a valid JWT.
You may obtain a JWT by authenticating using the login endpoint (see the auth section below).
To set up a user and permission see the [RBAC authorization section]({{% relref "/08-stackgres-web-api/02-rbac-authorization" %}}).

{{< sg-swaggerui >}}
