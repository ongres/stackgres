<div align="center">
   <h1>StackGres</h1>
   <p><b>Enterprise Postgres made easy. On Kubernetes</b></p>
   <a href="https://stackgres.io" target="_blank">
      <img src="https://stackgres.io/img/favicon/android-chrome-192x192.png" alt="StackGres"/>
   </a>
</div>

StackGres is a full-stack PostgreSQL distribution for Kubernetes, packed into an easy deployment unit.
With a carefully selected and tuned set of surrounding PostgreSQL components.

An enterprise-grade PostgreSQL stack needs several other ecosystem components and significant tuning.
It's not only PostgreSQL. It requires connection pooling, automatic failover and HA, monitoring,
backups and DR, centralized logging… we have built them all: a Postgres Stack.

Postgres is not just the database. It is also all the ecosystem around it. If Postgres would be the
Linux kernel, we need a PostgreSQL Distribution, surrounding PostgreSQL, to complement it with the
components that are required for production deployment. This is what we call a PostgreSQL Stack.
And the stack needs to be curated. There are often several software for the same functionality. And
not all are of the same quality or maturity. There are many pros and cons, and they are often not
easy to evaluate. It is better to have an opinionated selection of components, that can be packaged
and configured to work together in a predictable and trusted way.


## Getting Started

We recommend that you check our [documentation](https://stackgres.io/doc/latest/) and have a look at the [Demo / Quickstart](https://stackgres.io/doc/latest/demo/quickstart/) section to know how to start using StackGres.

Also, on our web https://stackgres.io/install/, you can get the one-line command to install StackGres.

## Features

- [Automated failover and High Availability with Patroni](https://stackgres.io/features/#automated-failover)
- [You are in full control. You are the postgres user](https://stackgres.io/features/#full-control)
- [Automated backups, lifecycle management](https://stackgres.io/features/#automated-backups)
- [Fully-featured management Web Console](https://stackgres.io/features/#web-console)
- [Automatic Prometheus integration. Built-in customized Grafana dashboards](https://stackgres.io/features/#prometheus-integration)
- [Distributed logs for Postgres and Patroni](https://stackgres.io/features/#distributed-logs)
- [High-level management CRDs. GitOps ready](https://stackgres.io/features/#management-crds)
- [Integrated server-side connection pooling](https://stackgres.io/features/#connection-pooling)
- [Enhanced observability via Envoy Proxy’s Postgres filter](https://stackgres.io/features/#envoy-proxy)
- [Expertly tuned by default](https://stackgres.io/features/#expertly-tuned)
- [Lightweight, secure container images based on RedHat’s UBI 8](https://stackgres.io/features/#redhat-based)

## Community

Everybody is welcome in this open Community for StackGres, check out the different ways to collaborate:

- [Developer Documentation](https://stackgres.io/doc/latest/developer/): If you are a Kubernetes Administrator or a Java Developer, you might want to contribute back to StackGres, either by testing it, developing Kubernetes code (Helm, architecture) or Java’s core operator.
- [Public dashboards](https://gitlab.com/ongresinc/stackgres/-/issues): If you want to see what’s coming down the road, check our issues. They are open so you can create issues yourself or comment on open issues. We welcome feedback, ideas and collaborations!
- Join the [Slack](https://slack.stackgres.io/) & [Discord](https://discord.stackgres.io/) Community becoming an advocate, user, tester or contributor of StackGres.

### Code of Conduct 

To ensure a more open and welcoming community, StackGres adheres to a [Code of Conduct](CODE_OF_CONDUCT.md), and everyone involved in the projects, issues, chat rooms, and any other official communication channel, is expected to follow it.

[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

## About the Operator

This repository holds one of the major components around StackGres and is the StackGres Operator
build around Kubernetes. An Operator is a method of packaging, deploying and managing a Kubernetes
application. Some applications, such as databases, required more hand-holding, and a cloud-native
Postgres requires an operator to provide additional knowledge of how to maintain state and integrate
all the components.

This operator is built in pure-Java and uses the [Quarkus](https://quarkus.io/) framework a Kubernetes
Native Java stack tailored for GraalVM & OpenJDK HotSpot, crafted from the best of breed Java
libraries and standards.

The container image of StackGres is built on Red Hat Universal Base Image and compiled as a native binary
with GraalVM allowing amazingly fast boot time and incredibly low RSS memory.

---

## FAQ

### Is StackGres a modified version of Postgres?
No. StackGres contains PostgreSQL, plus several other components (such as connection pool or
automatic high availability software) from the PostgreSQL ecosystem. All of them are vanilla
versions, as found in their respective open source repositories, including PostgreSQL.
Any application that runs against a PostgreSQL database should work as-is.

### How is StackGres software licensed?
StackGres source code is licensed under the OSI-approved open source license
GNU Affero General Public License version 3 (AGPLv3). All the source code is available on this
repository.

### Is there a StackGres commercial license that is “GPL-free”?
Yes. Contact us if you want a trial or commercial license that does not contain the GPL clauses.
Will you ever switch from an open-source license to a source-available one?
Our promise is that no, this won’t happen. We respect others who switch to or are directly built
as source-available software, but we don’t follow this approach.
We love the concept of GitLab’s stewardship, and in the same spirit, we promise here that
StackGres will always be open source software.

### What PostgreSQL versions are supported?
As of now, PostgreSQL major version 12 and 13. As a general rule, StackGres will support the last 2 Postgres version. 

### Where does it run?
StackGres has been designed to run on any Kubernetes-certified platform. Whether is a
Kubernetes-as-a-Service offered by a cloud provider or a distribution running on-premise,
StackGres should run as-is.

### How is HA implemented?
High Availability and automatic failover are based on Patroni, a well-reputed and trusted software
for PostgreSQL. No external DCS (Distributed Consistent Storage) is required, as it relies on
K8s APIs for this (which in turns reach etcd).

### Why is used UBI as the base image for StackGres?
Red Hat Universal Base Images (UBI) are OCI-compliant container base operating system images with
complementary runtime languages and packages that are freely redistributable. UBI lets developers
create the image once and deploy anywhere using enterprise-grade packages. For more information read
the official [UBI-FAQ](https://developers.redhat.com/articles/ubi-faq/).

---

```
   _____ _             _     _____
  / ____| |           | |   / ____|
 | (___ | |_ __ _  ___| | _| |  __ _ __ ___  ___
  \___ \| __/ _` |/ __| |/ / | |_ | '__/ _ \/ __|
  ____) | || (_| | (__|   <| |__| | | |  __/\__ \
 |_____/ \__\__,_|\___|_|\_\\_____|_|  \___||___/
                                  by OnGres, Inc.

```
