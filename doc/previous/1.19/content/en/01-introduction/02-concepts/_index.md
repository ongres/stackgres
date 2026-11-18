---
title: "Concepts"
weight: 2
url: /intro/concepts
description: Details about StackGres' concepts.
showToc: true
---

StackGres is a full-stack, production-grade [PostgreSQL](https://www.postgresql.org/) distribution for [Kubernetes](https://kubernetes.io/).
StackGres provides all features and management options required for running PostgreSQL in production, and on top of that it ships with sensible default options.

The name "StackGres" is derived from the "stack of components" that are required to run production-grade PostgreSQL instances.

On this page, we'll introduce the important concepts of StackGres.


## Production-Grade Database Management

Production-ready database management is at the core of StackGres.
StackGres enables all common (and some uncommon) database management operations in an easy, declarative way.
While doing so, StackGres sticks to production-grade behavior.
This means that certain operations aren't just blindly followed (for example when a user updates the target state of a database instance), but in a way that minimizes disruption of applications and users, in the same way as a good DBA would.

For example, when you change the PostgreSQL version or configuration, StackGres performs a rolling update that maintains cluster availability throughout the process.

So you can think of StackGres as the Kubernetes version of your friendly DBA – just with a Kubernetes API, much faster response time, and fewer coffee breaks.


## Kubernetes Centered

StackGres comes as a Kubernetes-based platform that provides production-grade PostgreSQL in the form of a Kubernetes operator.
So everything StackGres is heavily tied to Kubernetes.

> A Kubernetes operator is a method of packaging, deploying, and managing a Kubernetes-based application or platform.
> Some workloads, such as databases, require more hand-holding, and a cloud-native Postgres deployment requires additional knowledge of how to maintain state and integrate all the components.
> The StackGres operator allows you to deploy a StackGres cluster using a few custom resources created by the user.

Besides that, StackGres also follows the usability and look-and-feel that engineers know and like about using Kubernetes.
User-defined StackGres resources are meant to be used in the same convenient declarative model that we know from Kubernetes.
That is, the user defines a desired target state (usually as YAML, although other options are possible), and the StackGres operator does the heavy lifting as to how to get to that target state.


## Single Point of Access -- Multiple Ways

The StackGres platform represents a single point of access for all operations related to Postgres.
Users have multiple ways to access and modify the Postgres clusters, namely via Kubernetes API (e.g. using `kubectl`), REST API, or web UI.
No matter which ways or which combinations thereof are chosen, StackGres ensures that the status and all operations are always consistent.

This gives users the maximum of flexibility and simplicity.
For example, if a StackGres setup is operated by three users, one of which uses `kubectl` for everything, the second their own scripts using `curl`, and the third prefers a UI, all of them can perform the same actions and can access the same information.
All different ways enable the full range of features.


## Sidecar-Provided Features

The components of the StackGres "stack" are provided by sidecar containers that are deployed alongside the main Postgres container.

All container base images are built and provided by StackGres.
The lightweight and secure container images are based on RedHat's UBI 8.


## StackGres Clusters

When we refer to a "cluster", we are referring to a collection of PostgreSQL servers managed by StackGres.

A StackGres cluster (defined by the custom Kubernetes resource type `SGCluster`) thus is a replicated, managed, production-ready, and optimized PostgreSQL server.
A StackGres cluster comes with opinionated, reasonable configuration defaults, all biased towards a production-grade experience.
So, for a working setup, defining a StackGres cluster is already sufficient.
