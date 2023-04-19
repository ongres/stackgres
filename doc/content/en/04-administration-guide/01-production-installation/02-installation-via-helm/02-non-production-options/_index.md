---
title: Non-Production Options
weight: 2
url: install/helm/nonproduction
aliases: [ install/prerequisites/nonproduction ]
description: Important notes for non-production options in the production environment.
---

There are certain [non-production options]({{% relref "06-crd-reference/01-sgcluster" %}}#non-production-options) supported in StackGres, that we recommend to disable in a production environment.
To disable all of these options, create a YAML values file to include in the helm installation (`-f` or `--values` parameters) of the StackGres operator similar to the following:

```yaml
nonProductionOptions: {}
```

For reference, you can see a list of all of these [non-production options]({{% relref "06-crd-reference/01-sgcluster" %}}#non-production-options).