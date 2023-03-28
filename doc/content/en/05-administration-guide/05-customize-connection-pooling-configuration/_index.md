---
title: Customize Connection Pooling Configuration
weight: 5
url: administration/cluster/pool
description: Details about how to update the pooling configuration.
showToc: true
---

If you happen to be reading this, it's because you are aware of your application characteristics and needs for scaling connections on a production environment.

A simple way to target this correctly, is to verify the usage of Prepared Statements, on top of which `session` mode will be the only compatible.

Some applications, do not handle connection closing properly, which may require to add certain timeouts for releasing server connections.


## Reloading configuration

In the [Customizing Pooling configuration section]({{% relref "06-crd-reference/04-sgpoolingconfig/#pgbouncer" %}}),
 it is explained the different sauces for scaling connections properly.

Each configuration, once applied, need to be _reloaded_.
This can be done by getting the corresponding primary node pod name and issue the same signal it is done on most of the environments:

```
PRIMARY=$(kubectl get pod -l role=master -n my-cluster -o name)
kubectl exec -n my-cluster -it ${PRIMARY} -c postgres-util -- pkill --signal HUP pgbouncer
```

Check the pages below to know more about it: 

{{% children style="li" depth="1" description="true" %}}
