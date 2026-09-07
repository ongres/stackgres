+++
title = "AWS announces Babelfish: open source Postgres with SQL Server compatibility"
date = 2020-12-01T20:03:02+02:00
author = ["aht"]
tags = ["postgresql", "aws"]
planet = "aht"
draft = false
readtime = "5 min"
thumbnail = "img/blog/postgres_sql_server_compatibility.jpg"
description = "AWS announces Babelfish, open source SQL Server compatibility for Postgres, and why it is a major step toward widening the Postgres user base."
+++


## AWS announces Babelfish: open source Postgres with SQL Server compatibility

At [AWS re:Invent](https://reinvent.awsevents.com/)'s first keynote, Andy Jassy has announced a major breakthrough for Postgres: **SQL Server
compatibility for Postgres. Open Source**.

![Andy Jassy announces Babelfish](../../img/blog/babelfish_announced.png)


Postgres is a fantastic database: reliable, trustable, featureful. But has Postgres made a significant dent into the market share of commercial
databases? It has, but not at the level I would love to see it happen. Key to making Postgres more mainstream is to **widen Postgres userbase**. There
are many ways to achieve this goal, including developing better tools and making it more accessible to non experts. Other means to achive this goal is
what this announcement is doing today: adding compatibility with other databases, the commercial SQL Server database in this case.

From a technical perspective, this is a significant achievement. First of all, it adds the capability to bridge the SQL differences and features
between SQL Server and Postgres. While SQL is a standard, almost all databases deviate from it adding their own capabilities. [Modern
SQL](https://modern-sql.com/) website by Markus Winand tracks in detail the different SQL features of most common relational databases, if you are
interested. This compatibility layer is, thus, doable but not easy.

Second, SQL Server users frequently use a stored procedure langauge called T-SQL. It allows you to push "computation to the data", and run data logic
on the database side. It is so pervasive, that a compatibility capabily would not be complete without supporting T-SQL. Well, Babelfish comes with it,
too.

But if it weren't enough, SQL Server equivalent catalogs and also the SQL Server wire protocol (TDS) has been implemented. **This means that you will
be able to talk to Postgres Babelfish as if it were SQL Server: the same drivers, the same SQL, the same T-SQL and the same apps should work
unmodified**.

This is how Babelfish Postgres works:

![Babelfish Postgres architecture](../../img/blog/bbfsh-diagram.png)

Source: [babelfish-for-postgresql.github.io](https://babelfish-for-postgresql.github.io/babelfish-for-postgresql/)


I'm waiting to get a hold onto it. Compatibility will not be 100% at launch. But as long as it is high enough, it will enable seamless transition from
a commercial and proprietary database to the open source Postgres for thousands, millions of users. And the approach taken for correctness (if a
feature is not supported, an error is raised, no hackish implementations or partial implementations are provided) is sound and builds trust on
the success of the migrations. Pardon, replacements. **SQL Server users: you are welcome to the Postgres world, feel yourself at home**.


There was only one thing more to make this announcement truly huge: to make this open source. And that's exactly what AWS committed to do: **Babelfish
will be published as Open Source, under the Apache 2 License, on GitHub, in 2021**. AWS could have decided to keep this proprietary, and use it to its
sole advantage as part of AWS Aurora offering. However, it has been opened to the Community. You can [read more from Matt Asay on the AWS open
blog](https://aws.amazon.com/blogs/opensource/want-more-postgresql-you-just-might-like-babelfish/).

There's one question left: will this be integrated back into main PostgreSQL codebase, or will it live as an independent repository? I would love to
see both projects converge. A lot of exciting conversation will need to happen. But whatever the outcome is, this is a significant advance to
Postgres, targeting a clear market need, and a very welcome addition. I for one applaud making it open source.
