---
title: Non production options
weight: 5
url: install/prerequisites/nonproduction
description: Important notes for Non production options in the production environment.
---

We recommend to disable all non production options in a production environment. To do so create a
 YAML values file to include in the helm installation (`-f` or `--values` parameters) of the
 StackGres operator similar to the following:

<!--more-->

```yaml
nonProductionOptions: {}
```
