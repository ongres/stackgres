---
title: Contributing Guide
weight: 6
url: /developer/stackgres/contrib
description: Details about how to contribute to StackGres.
---

StackGres is open source, and as such we welcome any external contribution, in the form of feedback, testing, resources, documentation and, of course, code.
Merge requests are always welcome.

Please observe the following rules when contributing to StackGres:

* [Create an issue](https://gitlab.com/ongresinc/stackgres/issues/new) with any question or improvements about the source code, to keep the discussion organized.

* Contact us at stackgres at ongres dot com before sending a merge request, to have a contributor agreement signed with us.
  This is a requirement for your merge request to be merged upstream.

* Changes and merge requests should be performed from the `development` branch, instead of `main`.
  Please adhere as much as possible to the apparent style of the code that you're editing.


## StackGres Documentation

Documentation-only patches are more than welcome too. Help us improve our documentation!

While most of the documentation structure should be self-explained, here's a quick guide on what are the main goals of some relevant sections:

* [Getting Started]({{% relref "03-demo-quickstart" %}}). This section documents the simplest way to get StackGres installed and a cluster up and running.
  A new user should take no more than 10-30 minutes going through this demo section.
  It documents how to install StackGres from scratch, using `kubectl apply -f $URL`, how to create a simple cluster, how to connect to Postgres, and how to access the web console.

* [Administration Manual]({{% relref "04-administration-guide" %}}). A detailed, throughout guide about the StackGres features, including a production-grade installation and the installation options.
  This section targets cluster administrators and explains the StackGres features, how they work, and how to use them, with all possible options.
  The section documents them using both `kubectl` and the web console.

* [Runbooks]({{% relref "09-runbooks" %}}). This section documents runbooks, step-by-step guides to perform specific actions or scenarios on top of StackGres.
