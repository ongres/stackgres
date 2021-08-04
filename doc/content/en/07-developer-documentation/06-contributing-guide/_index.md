---
title: Contributing guide
weight: 6
url: developer/stackgres/contrib
description: Details about how to contribute.
---

StackGres is open source, and as such we welcome any external contribution, in the form of feedback, testing, resources,
 documentation and, of course, code. Merge requests are always welcome.

Please observe the following rules when contributing to StackGres:

* [Create an issue](https://gitlab.com/ongresinc/stackgres/issues/new) with any question or improvements about the source
 code, to keep the discussion organized.

* Contact us at stackgres at ongres dot com before sending a pull request, to have a contributor agreement signed with us.
 This is a requirement for your merge request to be merged upstream.

* Changes and merge requests should be performed from the development branch, instead of master. Please adhere as much
 as possible to the apparent style of the code you are editing.


## StackGres Documentation

Documentation-only patches are more than welcome too. Help us improve our documentation!

While most of the documentation structure should be self-explained, here's a quick guide on what are the main goals of
some relevant sections:

* [Demo / Quickstart]({{% relref "02-demo-quickstart" %}}). This section should document the simplest way to get
  StackGres installed and a cluster up and running. A new user should take no more than 10-30 minutes going through this
  demo section. It should document installing StackGres from `kubectl apply -f $URL`, install no prior dependencies,
  create a simple cluster and show how to connect to Postgres and how to access the Web Console.

* [Tutorial]({{% relref "03-tutorial" %}}). A much more in-depth step-by-step guide on how to install and use StackGres.
  It should be targetting a 1-3h duration for a new user. It may explain how to install some pre-requisites, but without
  going into deep details nor explaining all the code for all potential different environments. For that, it should
  appropriately reference the production installation and/or administration guides. Installation should be using Helm,
  documenting only the basic parameters used. Then showcase relevant StackGres features, but not all nor explaining them
  all in detail. Always point to or reference the production section for more details.

* [Production Installation]({{% relref "04-production-installation" %}}). A complete guide on StackGres installation.
  Exhaustively documenting all the pre-requisites, and all the prior configuration required on every supported
  Kubernetes environment, if any. Explaining and documenting all Helm options. And documenting in detail all the steps
  towards a successful and complete installation. The only production installation method considered is Helm.

* [Administration Guide]({{% relref "05-administration-guide" %}}). The equivalent of a full Administrator's
  Guide. A detailed, thorogh guide on every feature StackGres has. Explaining for each of them what they are, how they
  work, and how to use them, with all possible options. It should document them all using both `kubectl` and the Web
  Console.

* [Runbooks]({{% relref "09-runbooks" %}}). This section is to document runbooks, or steps/algorithms to perform actions
  on StackGres that may not be fully automated or supported via the StackGres declarative CRDs options, but that as a
  user you may perform on your own, or layering on top of StackGres.
