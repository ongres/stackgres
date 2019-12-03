---
title: "Architecture"
weight: 3
---

# Architecture

## The Operator

An Operator is a method of packaging, deploying and managing a Kubernetes
application. Some applications, such as databases, required more hand-holding, and a cloud-native
Postgres requires an operator to provide additional knowledge of how to maintain state and integrate
all the components. The StackGres operator allow to deploy a StackGres cluster using a few custom
resources created by the user.
