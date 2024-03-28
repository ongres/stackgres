---
Title: Run strace command inside pod container
weight: 10
url: /runbooks/strace-pod-container
description: How to run strace command inside StackGres cluster pod container
---

This runbook will show you how to run `strace` command inside a StackGres cluster pod container.

**1 - Open a new shell with patroni image:**

```
kubectl run -q --stdin --tty --attach --restart=Never --rm -it --overrides '{"spec":{"securityContext":{"runAsUser":0}}}' --image quay.io/ongres/patroni:v3.2.0-pg15.5-build-6.29 --command=true test -- sh
```

**2 - In the shell that opens, do:**

```
microdnf install strace
tar cf strace.tar /usr/bin/strace /usr/lib64/libdw*
```

**3 - In another shell outside of container without exiting from previous shell (the test Pod must stay alive):**

```
kubectl cp test:/strace.tar /tmp/strace.tar
kubectl cp /tmp/strace.tar -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME"-0:/tmp/strace.tar  -c patroni
kubectl exec -ti -n "$CLUSTER_NAMESPACE" "$CLUSTER_NAME"-0 -c patroni -- bash -c 'cd /tmp; tar xf strace.tar; LD_LIBRARY_PATH=/tmp/usr/lib64 /tmp/usr/bin/strace postgres --describe-config'
```