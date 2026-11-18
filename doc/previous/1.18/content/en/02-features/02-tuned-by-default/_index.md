---
title: Tuned by Default
weight: 2
url: /features/tuned-by-default
description: Expertly-tuned PostgreSQL by default
---

StackGres clusters will be created with a carefully tuned initial Postgres configuration, curated by the highly expert OnGres Postgres DBA team.

This means that StackGres ships with a tuned configuration out of the box, and you will be covered well enough with this default configuration, even if you are not a Postgres advanced user.
With StackGres, you don't need to be a Postgres expert to operate production-ready clusters.

If you prefer to further tune Postgres, you can create custom configuration via the [SGPostgresConfig CRD]({{% relref "06-crd-reference/03-sgpostgresconfig" %}}) and reference them in your clusters, accordingly.

> [OnGres](https://ongres.com/), the creators of StackGres are obsessed with tuning Postgres adequately.
> So much that they have built [postgresqlCO.NF](https://postgresqlco.nf/), a website that helps hundreds of thousands of Postgres users on how to better tune their database.

## Learn More

- [PostgreSQL Configuration Guide]({{% relref "04-administration-guide/04-configuration/02-postgres-configuration" %}})
- [SGPostgresConfig Reference]({{% relref "06-crd-reference/03-sgpostgresconfig" %}})