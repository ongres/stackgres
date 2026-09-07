+++
title = "CONF: PostgreSQL configuration for humans"
date = 2020-01-27T18:09:18+01:00
draft = false
tags = ["postgresql", "conf", "postgresql.conf"]
author = ["jorsol", "olesya"]
planet = "jorsol"
thumbnail = "img/blog/post_conf-v1.jpg"
description = "PostgresqlCO.NF is postgresql.conf documentation for humans: understand, tune and optimize the nearly 300 PostgreSQL configuration parameters."
+++

## What is PostgresqlCO&#46;NF?

[PostgresqlCO&#46;NF](https://postgresqlco.nf) (**CONF** for short) is your **postgresql.conf**
documentation and ultimate recommendations' source. Our mission is to help you tune and optimize all
of your PostgreSQL configuration. With around 290 configuration parameters in postgresql.conf
(and counting), it is definitely a difficult task! We aim to make PostgreSQL configuration accessible
for HUMANS.

<!--more-->

**CONF** is, according to our users, “*literally the best Postgres configuration reference ever
created.*”

[See what our users say about it on Twitter →](https://twitter.com/i/status/1216800447740047360)

## Why you should be using CONF

There are almost 300 parameters and with each new Postgres version more are added. Parameters can be
_integers_, _real_, _string_, _enum_ or _bool_. Numeric values may have units (or unit-less) and some
units can be confusing (like “blocks of 8kB”). It can be challenging to try to understand them all.

You can learn many things by reading the documentation reference, but **CONF** aggregates the
official documentation, plus some recommendations and relevant links to StackOverflow and the
*-hackers* mailing list in a direct and easy to read format that you can bookmark or share a link of
an specific parameter to someone, and if you already are an expert you can comment on the site and
add your thoughts on how to tune any given parameter.

Some parameters require restart. Having a quick and clear reference of all the parameters context can
help you to plan ahead whether you need or not a maintenance window for your deployment. Consider
archive_mode parameter as an example: it's required for WAL archiving but it's `off` by default
and requires restart; the archive_command on the other hand does not require restart. Actually, one
good recommendation is to set archive_mode to `on` and archive_command to `/bin/true` until you find
the real archive_command you will use in production, but never have to restart for this reason. This
is just the tip of the iceberg on what you can learn by just reading the docs.

However **CONF** it's not just a documentation reference. Right now we are working hard on a fully
featured application service where you can have a graphical configuration interface (or UI) with
Drag & Drop of your postgresql.conf files with automatic validation, and a REST API where you can
store and share your custom postgresql.conf configuration files. You will also be able to download
your configurations in several formats, like the native postgresql.conf, YAML or JSON.

> Subscribe to our extremely low traffic mailing list on https://postgresqlco.nf to become a beta
> tester of the configurator when the software will be ready.

But wait, there's more.  There is a little hidden feature that we want to make public today. It is
a small JavaScript snippet that you can add to your web/blog that converts all the parameters
found in the html and shows a nice tooltip with info about that parameter. **Like magic!**

{{< figure src="https://postgresqlco.nf/assets/img/screenshot-snippet-conf.png" class="lightbox" >}}

Adding it to your website will take you 1 minute. Check the [documentation
page](https://postgresqlco.nf/en/snippet-usage/) and grab it while it's hot. We are improving it
with more options in later versions, but we can't wait to hear your comments and feedback (and of
course bugs reports).

## The redesign

On December 31, 2019, we deployed (yes, we deployed on 12/31, it wasn't Friday but it's pretty much
the same thing!) a new version of the postgresqlCO&#46;NF website, saying goodbye to both the old
year and design. We had been working on the redesign of our Postgres Documentation page for a while.

First of all, we decided to give CONF a more modern look, changing the UI color scheme and unifying
all fonts and sizes. Now, all pages have the same  look and feel: clean, professional and most
importantly, intuitive. We believe it is important that these services are not only useful, but also
pleasant and easy to use. We kept the three-column structure, but we reorganised the content.
This redesign also fixes many small bugs that existed in the previous version.

Last but not least, the whole infrastructure that runs CONF has changed. We migrated from a web
application powered by Java that dynamically renders the pages served from Google Cloud, to a
statically generated web site served from Amazon S3 using CloudFront. This drastically improves the
performance of the whole site, giving a better user experience.

Feel free to give us your feedback about this new version of CONF and stay tuned to our updates, as
we will be adding some new and highly requested features (like a dark mode :sunglasses:) really soon!

## Do you want to know where it all started?

{{< youtube id="IFIXpm73qtk" class="video-16x9" >}}
