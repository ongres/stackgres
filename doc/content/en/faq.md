---
title: FAQ
weight: 999
chapter: false
hidden: true
url: /faq
---

## Is StackGres a modified version of Postgres?

No. StackGres contains [PostgreSQL](https://www.postgresql.org/), plus several other components (such as connection pooling or automatic high availability software) from the PostgreSQL ecosystem. All of them are vanilla versions, as found in their respective open source repositories, including PostgreSQL. Any application that runs against a PostgreSQL database should work as-is if you use StackGres.

## How is StackGres licensed?

StackGres source code is licensed under the [OSI](https://opensource.org/licenses)-approved open source license [GNU Affero General Public License version 3](https://www.gnu.org/licenses/agpl-3.0.en.html) (AGPLv3). All the source code is available on this repository.

## Is there a “GPL-free” commercial license for StackGres?

Yes. Contact us at StackGres at Ongres doc com if you want a trial or commercial license that does not contain the GPL clauses.

## Will you ever switch from an open-source license to a source-available one?

No, this won’t happen. That's our promise. We respect others who switch to or are directly built as source-available software, but we don’t follow this approach. We love the concept of [GitLab’s stewardship](https://about.gitlab.com/company/stewardship/), and in the same spirit, we promise you that StackGres will always be open source software.

## What PostgreSQL versions are supported?

As of now, PostgreSQL major version 11 and 12. Version 13 will be added soon.

## Where can I run StackGres?

StackGres has been designed to run on any Kubernetes-certified platform. Whether that's a Kubernetes-as-a-Service offered by a cloud provider or running on-premise, StackGres should run as-is.

## How is HA implemented?

High Availability and automatic failover are based on [Patroni](https://github.com/zalando/patroni), a well-reputed and trusted software for PostgreSQL. No external DCS (Distributed Consistent Storage) is required, as it relies on K8s APIs for this (which, in turn, uses etcd internally).

## Is there connection pooling?

Yes, we use [pgbouncer](https://www.pgbouncer.org/). Most Postgres DBaaS solutions don't include connection pooling as part of their managed service. Yet, in most real-life scenarios, PostgreSQL should be fronted by a connection pooler. There are many reasons for this, but the main ones are excessive memory consumption and degraded performance under too many connections — where too many can be as low as several hundreds or even a few thousand. That's why we include it in StackGres.

## What “OS” are container images based on? Why not Alpine?

All StackGres container images are built on the [Red Hat Universal Base Image](https://developers.redhat.com/products/rhel/ubi/) (UBI) version 8, which is derived from [RHEL 8](https://www.redhat.com/en/enterprise-linux-8). Red Hat Universal Base Images (UBI) are OCI-compliant container base operating system images with complementary runtime languages and packages that are freely redistributable. UBI lets developers create the image once and deploy anywhere using enterprise-grade packages. For more information read the official [UBI-FAQ](https://developers.redhat.com/articles/ubi-faq/).
Alpine images are even smaller than UBI. However, they have significant disadvantages. They:

- Use musl libc, which might trigger performance and/or compatibility problems with PostgreSQL and other components of its ecosystem, including third-party extensions.
- Don't have a trusted and long-term roadmap as Red Hat does.
- Don't have third-party support, whereas UBI images can be supported with an existing RHEL support contract.

## Does StackGres have any affiliation with Red Hat?

No. We just believe UBI are the best base images for enterprise-grade containers. This way, StackGres users can get support for the container images from the most popular Linux distribution.

## Can I get support for StackGres?

Yes. [Contact with us](https://stackgres.io/contact/).

## Will StackGres support other databases that are not PostgreSQL?

No. [OnGres](https://ongres.com/) (“On Postgres”), the company behind StackGres, is a Postgres-only shop. That’s our expertise and there’s no plan to divert from this. Databases are a very complex world, and while they may "look and feel" the same from a user perspective, they are very different from an operational perspective. We have more than two decades of experience developing and running PostgreSQL databases. Plus we believe PostgreSQL is the best relational database, anyway!

## Any other question?

If you think we should add answers to other questions, please [file an issue on our repository](https://gitlab.com/ongresinc/stackgres/issues/new)!
