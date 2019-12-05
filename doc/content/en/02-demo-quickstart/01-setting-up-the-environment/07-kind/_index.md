---
title: "Kind"
weight: 7
---

# Kind

[Kind](https://kind.sigs.k8s.io/) is a tool for running local Kubernetes clusters using Docker container “nodes”.
kind is primarily designed for testing Kubernetes 1.11+, initially targeting the conformance tests.

1. Download kind & install executable:

```
sudo wget -q -L -O /usr/local/bin/kind  https://github.com/kubernetes-sigs/kind/releases/download/v0.5.1/kind-$(uname)-amd64
sudo chmod a+x /usr/local/kind
```

2. Create a kind cluster

```
kind create cluster
```

3. Install NFS utility for backups:

This is required in order to store PostgreSQL backups in a shared NFS disk.

```
docker exec -ti kind-control-plane sh -c 'DEBIAN_FRONTEND=noninteractive apt-get update -y -qq < /dev/null > /dev/null'
docker exec -ti kind-control-plane sh -c 'DEBIAN_FRONTEND=noninteractive apt-get install -y -qq nfs-common < /dev/null > /dev/null'
```
