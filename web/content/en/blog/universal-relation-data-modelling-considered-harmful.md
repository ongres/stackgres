+++
title = "Universal Relation Data Modelling Considered Harmful"
date = 2020-10-28T19:41:24+02:00
author = ["aht"]
tags = ["postgresql", "nosql", "dynamodb"]
planet = "aht"
draft = false
readtime = "3 min"
thumbnail = "img/blog/post-ur-modelling.jpg"
description = "Why Universal Relation data modelling, a.k.a. Single Table Design in DynamoDB, is harmful: a benchmark and analysis with Dr. Michael Stonebraker."
+++


## Universal Relation Data Modelling Considered Harmful

As part of a continued series of blog posts themed about "Relational databases and NoSQL",
Dr. Michael Stonebraker and I co-authored a new blog post just published on
[EnterpriseDB's blog](https://www.enterprisedb.com/blog/universal-relation-data-modelling-considered-harmful).

This post discusses the "Universal Relation" data model, which has become trendier recently under
the name of "Single Table Design", and is frequently used and recommended in DynamoDB. We discuss a sample
DynamoDB application, and run a benchmark analyzing potential conflicts with DynamoDB transactions.
All source code is [open source and
public](https://gitlab.com/ahachete/blog-posts-src/-/tree/master/202009-universal_relation/src). The
source [also contains a trivial exercise](https://gitlab.com/ahachete/blog-posts-src/-/blob/master/202009-universal_relation/src/relational.sql),
not detailed in the blog post, on how to emulate Single Table Design with Postgres views. Not of
real utility, but shows Postgres flexibility.

Previous post on this serie are:
* [Comparison of JOINS: MongoDB vs. PostgreSQL](https://www.enterprisedb.com/blog/comparison-joins-mongodb-vs-postgresql)
* ["Schema Later" Considered Harmful](https://www.enterprisedb.com/blog/schema-later-considered-harmful)

