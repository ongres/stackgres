---
title: StackGres REST API Reference
weight: 3
layout: swagger
url: api/reference
aliases: [ /08-stackgres-web-api/03-api-reference/ ]
description: Details about StackGres' REST API and its endpoints.
---

The StackGres REST API offers a way to interact with StackGres in a similar way as using the
 kubernetes API through the StackGres CRDs but with more functionality, like creating secrets
 and configmap in the same call and leveraging the [kubernetes RBAC permission system]({{% relref "/08-stackgres-web-api/02-rbac-authorization" %}}).

The API is provided by a kubernetes deployment that is installed together with the operator.
 It is mainly used by the Web UI that run in the same deployment.

Swagger is in essence an Interface Description Language for describing RESTful APIs expressed
 using JSON. Swagger is used together with a set of open-source software tools to design,
 build, document, and use RESTful web services. Swagger includes automated documentation,
 code generation (into many programming languages), and test-case generation.

To access those endpoints that require authentication you will need a valid JWT. You may obtain
 the JWT by authenticating using the login endpoint (see AUTH section above). To set up a user
 and permission see the [RBAC authorization section]({{% relref "/08-stackgres-web-api/02-rbac-authorization" %}}).

{{< sg-swaggerui >}}
