---
title: Cookbook
weight: 5
chapter: true
url: /cookbook
---

### Chapter 5

# Cookbook

This chapter is a catalog of recipes for getting things done with StackGres. Each
recipe documents a single capability: what it does, when to use it, how to drive it
through the Kubernetes API and the StackGres custom resources, how it works under the
hood, what to expect once applied, and the pitfalls you may run into.

Recipes are organized around the lifecycle of a Postgres cluster:

{{% children style="li" depth="1" description="true" %}}

The split matters because StackGres enforces it. Some capabilities can only be set when
the cluster is first created — the validating webhook rejects attempts to change them on
a running cluster. Those live under [Creating clusters]({{% relref "05-cookbook/01-creating-clusters" %}})
as variants of a basic creation. Everything that can be reconciled on a live cluster
lives under [Operating clusters]({{% relref "05-cookbook/02-operating-clusters" %}}).
