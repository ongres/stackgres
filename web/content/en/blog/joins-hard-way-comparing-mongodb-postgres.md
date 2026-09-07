+++
title = "JOINs the hard way: comparing MongoDB and Postgres"
date = 2020-04-06T20:20:14+02:00
draft = false
tags = ["postgres", "postgresql", "mongodb", "mongo", "performance"]
author = ["aht"]
planet = "aht"
description = "Comparing JOINs in MongoDB and Postgres with Dr. Michael Stonebraker: embedded vs reference data models on a classic employee-department schema."
+++


## JOINs the hard way: comparing MongoDB and Postgres

Dr. Michael Stonebraker and I have just published a new blog post on our series comparing MongoDB
and Postgres. This second post compares
[JOINing data in MongoDB and Postgres](https://www.enterprisedb.com/blog/compare-mongodb-vs-postgresql-join-command),
and follows on our previous post,
["Schema Later" Considered Harmful](https://www.enterprisedb.com/blog/schema-later-considered-harmful).

This new post analyzes how to structure data in both systems to model a classical
employee-department data model. We analyze why the embedded model won't work for the Mongo case, and
why the reference model is brittle in this system. The post also includes a simple performance
benchmark, comparing both systems. Following on our best practices,
[it is open, public and anyone can reproduce it](https://gitlab.com/ahachete/pgmongojoins).

Our performance results were not surprising. On a given user group, last year, a user claimed:

> _If 500ms for a 1000x500 rows lookup is normal, thats very bad news for MongoDB in our project_

and one of MongoDB's top engineers [replied](https://groups.google.com/forum/m/#!topic/mongodb-user/DZ_7gpbz4Po):

> _That's because you are choosing to do a join_.

_Update_: discussion on Hacker News made it to the front page. [Join the conversation](https://news.ycombinator.com/item?id=22834036).
