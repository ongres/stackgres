---
title: "Kind"
weight: 7
---

[Kind](https://kind.sigs.k8s.io/) is a tool for running local Kubernetes clusters using Docker container "nodes".
kind is primarily designed for testing Kubernetes 1.11+, initially targeting the conformance tests.

1. Download kind & install executable:

    ```bash
    sudo wget -q -L -O /usr/local/bin/kind  https://github.com/kubernetes-sigs/kind/releases/download/v0.8.1/kind-$(uname)-amd64
    sudo chmod a+x /usr/local/bin/kind
    ```

2. Create a kind cluster

    ```bash
    kind create cluster --image kindest/node:v1.17.5
    ```

3. Install NFS utility for backups:

    This is required in order to store PostgreSQL backups in a shared NFS disk.

    ```bash
    docker exec -ti kind-control-plane sh -c 'DEBIAN_FRONTEND=noninteractive apt-get update -y -qq < /dev/null > /dev/null'
    docker exec -ti kind-control-plane sh -c 'DEBIAN_FRONTEND=noninteractive apt-get install -y -qq nfs-common < /dev/null > /dev/null'
    ```
