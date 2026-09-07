+++
title = "Repository, Tuning Guide and API for your postgresql.conf"
date = 2020-12-18T18:45:23+01:00
draft = false
tags = ["postgresql", "conf", "postgresql.conf"]
author = ["aht" ]
planet = "aht"
thumbnail = "img/blog/conf-manage-thumbnail.jpg"
description = "PostgresqlCO.NF grows beyond documentation with per-version parameter repositories, a tuning guide and an API for your postgresql.conf."
+++


## Repository, Tuning Guide and API for your postgresql.conf

[postgresqlco.nf](https://postgresqlco.nf) (aka **postgresqlCO.NF, or simply "CONF"**) was born a little bit more than
two years ago. CONF's main goal was to help Postgres users find more and easier help to understand and tune their
`postgresql.conf`s. Fast-forward to today, CONF has helped more than 150K users with their Postgres configuration
settings, and is on the way to serving 1M page views every year, growing at double digits month-over-month.

Up until today, CONF was "just" `postgresql.conf` documentation. Not any documentation, but documentation that offered:

* The official PostgreSQL documentation for each parameter, with a summary table including minimum, maximum or enum vales,
  units, whether parameter requires a restart, etc.

* Documentation localized in five languages: **[English](https://postgresqlco.nf/doc/en/param),
  [Japanese](https://postgresqlco.nf/doc/ja/param), [Russian](https://postgresqlco.nf/doc/ru/param),
  [Chinese](https://postgresqlco.nf/doc/zh/param) and [French](https://postgresqlco.nf/doc/fr/param)**.

* Stable URLs that you can use in your blog posts and documents, with specific URL params for selecting the version and
  language you want for the parameter link. E.g.:
  [https://postgresqlco.nf/doc/ru/param/work_mem/9.6/](https://postgresqlco.nf/doc/ru/param/work_mem/9.6/).

* Recommendations generously provided by [annotated.conf](https://github.com/jberkus/annotated.conf).

* For each parameter, links to relevant mails in `-hackers` and comments in StackOverflow.

* The option to leave your own comments for every parameter, and read other user's comments.


See [our previous post about CONF](https://stackgres.io/blog/postgresqlconf-configuration-for-humans/) for more
details. CONF has proven quite useful for many people, and has turned CONF into a reference site for Postgres parameter
documentation. But that wasn't enough. CONF's vision goes further beyond into helping Postgres users...


### New functionality: Repository, Tuning Guide and API for your postgresql.conf

Available today (in beta), CONF is adding a new significant functionality:
**[manage your `postgresql.conf` files](https://postgresqlco.nf/manage)**. Think of a SaaS service which you can rely
on to **upload, download, edit, validate and share publicly (if you want) your configuration files**. In particular, as
of today CONF enables you to:

* Upload existing `postgresql.conf` files and store them in CONF, permanently.

* Create new configurations and/or edit existing configurations. The online editor provides you inline help about the
  parameter and instant validation: not out of range values, invalid units or incorrect syntax.

![Edit your postgresql.conf configurations online!](../../img/blog/conf-edit_configuration.png)

* Download any configuration in many available formats: `default` (`key=value` with links to the original parameter
  documentation), `minified` (smallest version), `original` (keeping comments and blanks as you uploaded them, suitable
  to keep original `postgresql.conf`s format), `ALTER SYSTEM` and of course `JSON` and `YAML`. The latters are suitable
  for integration and scripting.

* Share the configurations that you want, publicly. You will get a permanent URL. Viewers will be able to download in
  all the formats or clone into their account the configuration. Think of [Depesz](https://explain.depesz.com/) but for
  `postgresql.conf` instead of query plans.

* An API that you can use to integrate these functionalities into your deployment scripts, Ansible, Terraform, GUI
  management tools... you name it.

* A Tuning Guide. `postgresql.conf` tuning is not to be taken lightly. If parameters could be tuned based on simple
  rules, they would come tuned by default. But with each configuration, CONF provides you a "Tuning Guide", that will
  tour you on the criteria needed to tune the parameters that are recommended to be tuned. On three different
  "difficulty levels", so you chose how far you want to go.


![Tuning Guide for your postgresql.conf](../../img/blog/conf-tuning_guide.png)


* A [configuration snippet](https://postgresqlco.nf/manage/snippet-usage), that you can embed into any web page and have
  your (public) configuration rendered
  automatically. Perfect for blog posts. Avoid hardcoding values, and update them when needed without touching your blog
  post.

* Oh, and most importantly: **light and dark themes**!


CONF is a free service. Forever. Just go to [https://postgresqlco.nf](https://postgresqlco.nf) and start using it today!
We hope that **configuring and tuning Postgres will start being less and less of a difficulty for many**. We just ask
you in return to help spread the word. Happy `postgresqlco.nf` tuning!
