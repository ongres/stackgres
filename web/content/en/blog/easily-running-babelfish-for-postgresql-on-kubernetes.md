+++
title = "Easily Running Babelfish for PostgreSQL on Kubernetes"
date = 2021-11-15T20:05:34+01:00
author = ["aht"]
tags = ["postgres", "kubernetes", "babelfish"]
planet = "aht"
draft = false
thumbnail = "img/blog/easily-running-babelfish-for-postgresql-on-kubernetes-thumbnail.png"
description = "Babelfish is a SQL Server compatibility layer for Postgres. See how to run Babelfish on Kubernetes very easily: one command and 15 lines of YAML."
+++

# Easily Running Babelfish for PostgreSQL on Kubernetes

## TL;DR

Babelfish for PostgreSQL ("Babelfish" in short) is [an open source project](https://babelfishpg.org) created by Amazon AWS that adds SQL Server compatibility on top of Postgres. It was made public a couple of weeks ago both as a managed service and as an open source project. Using the latter involves as of today compiling it from source, which requires some effort and expertise. To contribute a better user’s experience, **the upcoming StackGres 1.1.0 release has added Babelfish, making it trivially easy to run Babelfish on Kubernetes**.

If, for any reason, you don’t have a Kubernetes cluster handy; and/or you are not familiar with Kubernetes, please jump first to the **Appendix in the last section, where I show how to get up & running a lightweight Kubernetes cluster in 1 minute**.


## Babelfish for PostgreSQL

Around one year ago, Amazon surprised us all by announcing **Babelfish for PostgreSQL**, a project that would bring a SQL Server compatibility layer on top of Postgres. Babelfish would become both an AWS managed service (on Aurora); as well as an open source project! I then [blogged about it](https://www.ongres.com/blog/aws_announces_open_source_postgres_with_sql_server_compatibility/), knowing that this was a disruption point for Postgres. Babelfish enables Postgres to reach out to many other use cases, users and Communities: the SQL Server ecosystem.

Adding yet another capability to Postgres reflects on the thoughts shared by Stephen O’Grady on a recent post, [A Return to the General Purpose Database](https://redmonk.com/sogrady/2021/10/26/general-purpose-database/). Postgres is not only a feature-full relational database; but with its extensions, it’s also a time-series database; a sharded database; a graph database; and now, also a SQL Server-compatible database. Postgres is, and will be, the unifying database for almost every imaginable database workload.

At StackGres we were almost literally eating our finger’s nails, waiting for Babelfish to be finally published as open source. It finally happened [a couple of weeks ago](https://babelfishpg.org/blog/releases/2021/10/babelfish-launch/). Since then, **our team has been working tirelessly to give you an easy way to run Babelfish on Kubernetes, by integrating Babelfish in StackGres. Kudos to the whole team for such an amazing job!**

Not only are we making Babelfish available on Kubernetes; but also giving to the Community the first (as far as we know) mechanism to run the open source Babelfish version without having to go through the (somehow involved) process of [compiling it from the source code](https://babelfishpg.org/docs/installation/compiling-babelfish-from-source/). **With StackGres, you can go from zero to Babelfish in one command to install StackGres; and a 15-lines YAML to create a Babelfish-enabled cluster. It can’t hardly get easier than this**.


## Install StackGres 1.1.0-beta1 with Babelfish support

Installation is just one command. The recommended installation method is with [Helm](https://helm.sh/):

```sh
$ helm install --namespace stackgres --create-namespace stackgres \
  https://stackgres.io/downloads/stackgres-k8s/stackgres/1.1.0-beta1/helm/stackgres-operator.tgz
```

In some seconds / a minute you should have StackGres installed. If you see the StackGres ascii-art logo, you are ready to go!


## Create a Babelfish cluster

Creating a Babelfish cluster is not so much different from creating a simple Postgres cluster. The few differences will be highlighted. Follow the instructions on either of the next two sections ("**Using kubectl**" or "**Using the Web Console**"), depending on your preferences. You can use either method interchangeably.

First, create a namespace:

```sh
$ kubectl create namespace notmssql
```


### Using kubectl

Create the following 15-line file `bbf.yaml` with the following content:

<pre class="language-yaml">
<span class="pink">kind</span>: SGCluster
<span class="pink">apiVersion</span>: stackgres.io/v1
<span class="pink">metadata</span>:
  <span class="pink">namespace</span>: notmssql
  <span class="pink">name</span>: bbf
<span class="pink">spec</span>:
  <span class="pink">instances</span>: 1
  <span class="pink">postgres</span>:
    <span class="pink">version</span>: <span class="yellow">'latest'</span>
    <b class="highlight"><span class="pink">flavor</span>: babelfish</b>
  <span class="pink">pods</span>:
    <span class="pink">persistentVolume</span>:
      <span class="pink">size</span>: <span class="yellow">'5Gi'</span>
  <b class="highlight"><span class="pink">nonProductionOptions</span>:
  <span class="pink">enabledFeatureGates</span>: [ <span class="yellow">"babelfish-flavor"</span> ]</b>
</pre>

The main differences between creating a Babelfish cluster and a regular (vanilla Postgres) cluster are:



* The `.spec.postgres.flavor` field must be set to `babelfish`. If unset, it is assumed the default "vanilla" Postgres flavor. Also note that the version: `latest` field automatically resolves to different versions, depending on the flavor (14.0 for the default Postgres flavor and 13.4 for Babelfish).

* Babelfish is in preview mode in StackGres. It is not considered production ready. As such, you need to explicitly enable a feature gate named `babelfish-flavor` or cluster creation would be rejected by StackGres. This is what the two last lines do.

Finally apply it with:

```sh
$ kubectl apply -f bbf.yaml
```

In around a minute, **you should get your Babelfish cluster running on StackGres**! It wasn’t hard, was it? You can check that the cluster is up and running:

```sh
$ kubectl -n notmssql get pods   	 
NAME    READY   STATUS      RESTARTS      AGE
bbf-0   6/6     Running     0             11m
```


### Using the Web Console

StackGres comes bundled with a fully-featured Web Console, that it’s installed by default. For simplicity, let’s use the port-forwarding mechanism of kubectl to expose it on a port local to your computer:

```sh
$ WEBC=`kubectl --namespace stackgres get pods -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}"`
$ kubectl --namespace stackgres port-forward "$WEBC" 8443:9443
```

Open in your web browser the address [https://localhost:8443](https://localhost:8443). You will see a warning (expected, the certificate is self-signed, you may also bring your own with custom Helm parameters). The default username is admin. The password is generated randomly and can be queried with the following command:

```sh
$ kubectl -n stackgres get secret stackgres-restapi --template '{{ printf "%s\n" (.data.clearPassword | base64decode) }}'
```

Select the notmssql namespace in the dropdown. And proceed to create a new SGCluster. Select the Babelfish flavor (you will see that the feature gate "Babelfish Flavor" becomes enabled). It should look like the following screenshot:


![Create a Babelfish cluster from the Web Console](/img/blog/create_babelfish_cluster-web_console.png)



## Connecting to your Babelfish cluster via the TDS protocol

Babelfish speaks the TDS protocol, like SQL Server. By default it listens in the same TCP port, 1433, and this is also what StackGres exposes.

To easily get connected using the TDS protocol, this StackGres release also includes with the `postgres-util` sidecar of every pod the command line utility [usql](https://github.com/xo/usql), a database client that supports many databases, including SQL Server. By default, StackGres installation creates a database named `babelfish`, owned by a superuser named `babelfish`, that is initialized to be connected via the TDS protocol, with all the required Babelfish extensions already loaded. The password is generated randomly and can be obtained from the secret named after the cluster name:

```sh
$ kubectl -n notmssql get secret bbf --template '{{ printf "%s" (index .data "babelfish-password" | base64decode) }}'
```

Knowing the password, we can trivially connect to Babelfish with usql, using the SQL Server protocol, and run some example **T-SQL** queries:

```sh
$ kubectl -n notmssql exec -it bbf-0 -c postgres-util -- usql --password ms://babelfish@localhost
Enter password:
Connected with driver sqlserver (Microsoft SQL Server 12.0.2000.8, , Standard Edition)
Type "help" for help.

ms:babelfish@localhost=> select @@version;
                               version
----------------------------------------------------------------------
 Babelfish for PostgreSQL with SQL Server Compatibility - 12.0.2000.8+
 Nov  3 2021 09:55:42                                                +
 Copyright (c) Amazon Web Services                                   +
 PostgreSQL 13.4 for Babelfish OnGres Inc. on x86_64-pc-linux-gnu
(1 row)

ms:babelfish@localhost=> create schema sch1;
CREATE SCHEMA
ms:babelfish@localhost=> create table [sch1].test (
ms:babelfish@localhost(> pk int primary key identity(1,1),
ms:babelfish@localhost(> text varchar(50),
ms:babelfish@localhost(> t datetime);
CREATE TABLE
ms:babelfish@localhost=> insert into  [sch1].test (text, t) values ('hi', getdate());
INSERT 1
ms:babelfish@localhost=> select * from [sch1].test;
 pk | text |            t
----+------+-------------------------
  1 | hi   | 2021-11-14T23:26:05.45Z
(1 row)
```


Above are shown commands using the T-SQL syntax, and a connection over the TDS protocol, with specific SQL Server data types and functions. But there’s no SQL Server, only Postgres! It would be interesting to see how this is seen by Postgres? Let’s give it a try!

```sh
$ kubectl -n notmssql exec -it bbf-0 -c postgres-util -- psql babelfish
psql (13.4 OnGres Inc.)
Type "help" for help.

babelfish=# \d master_sch1.test
                                   Table "master_sch1.test"
 Column |       Type        |       Collation       | Nullable |           Default
--------+-------------------+-----------------------+----------+------------------------------
 pk     | integer           |                       | not null | generated always as identity
 text   | sys."varchar"(50) | bbf_unicode_cp1_ci_as |          |
 t      | sys.datetime      |                       |          |
Indexes:
    "test_pkey" PRIMARY KEY, btree (pk)

babelfish=# table master_sch1.test;
 pk | text |            t
----+------+-------------------------
  1 | hi   | 2021-11-14 23:26:05.449
(1 row)
```

That was interesting! You can see the obvious differences between the different syntax and data types.

Finally, let’s try to access Babelfish from a GUI tool. Firstly, let’s use port-forward to expose our pod’s port 1433 on our own laptop:

```sh
$ kubectl --namespace notmssql port-forward bbf-0 1433:1433
```

Now you can use GUI tools to connect as if you had SQL Server on your laptop. Some may not work yet, as Babelfish progresses. Below is a screenshot of how I connected with [DBeaver](https://dbeaver.io/):


![Connecting to Babelfish with DBeaver, using the SQL Server protocol](/img/blog/babelfish_from_dbeaver.png)


Just please note to use the default’s `master` value for the Database/Schema. Querying the metadata table gave some errors, but general operation works well, and data is perfectly shown. **Enjoy running T-SQL on your Babelfish for PostgreSQL**.


## Conclusion

Please be aware that this is a beta version of the upcoming `1.1.0` release, and as such it is not as polished as a GA release. Even on 1.1.0GA, Babelfish will not be declared "production ready". Use it at your own risk.

However, **it is quite interesting to start exploring Babelfish, given that StackGres makes it so simple to use**. No need to go over the somehow involved compilation process that the current open source Babelfish release requires. **Just a few commands, and you get a fully working Babelfish version on Kubernetes**.

We’re eager to get feedback and ideas from you all. Please join our [Slack](https://slack.stackgres.io) and/or [Discord](https://discord.stackgres.io) channels and let you know what you think about Babelfish, StackGres, and chat about any other topic related to Postgres on Kubernetes, if you want.


## Appendix: If you don’t have Kubernetes, get it in 1 minute

Not totally familiar with Kubernetes yet still want to easily try Babelfish? Keep reading, you just need one additional minute.

Run on your laptop:

```sh
$ curl -sfL https://get.k3s.io | sh -
```

This will get you running with [K3s](https://k3s.io/), a lightweight, certified Kubernetes distribution by SUSE Rancher. K3s will get started as a service on your system (managed by systemd; you can later stop with `sudo systemctl stop k3s` and/or uninstall with `/usr/local/bin/k3s-uninstall.sh`). To get access to `kubectl`, you can get it via the k3s binary:

```sh
$ sudo k3s kubectl
$ alias kubectl="sudo k3s kubectl"	# optional
```

To check that the k3s cluster is running, you can run:

```sh
$ kubectl cluster-info
Kubernetes control plane is running at https://127.0.0.1:6443
CoreDNS is running at https://127.0.0.1:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
Metrics-server is running at https://127.0.0.1:6443/api/v1/namespaces/kube-system/services/https:metrics-server:/proxy

To further debug and diagnose cluster problems, use 'kubectl cluster-info dump'.
```

You are ready to go!
