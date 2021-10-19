---
title: "Kind"
weight: 7
url: demo/setenv/kind
description: kind is a tool for running local Kubernetes clusters using Docker container “nodes”.
---

[Kind](https://kind.sigs.k8s.io/) is a tool for running local Kubernetes clusters using Docker container "nodes".
kind is primarily designed for testing Kubernetes 1.11+, initially targeting the conformance tests.

1. Download kind & install executable:

    ```bash
    sudo wget -q -L -O /usr/local/bin/kind  https://github.com/kubernetes-sigs/kind/releases/download/v0.8.1/kind-$(uname)-amd64
    sudo chmod a+x /usr/local/bin/kind
    ```

2. Create a kind cluster

    For newcomers and testing purposes, we recommend to use the latest version available in the `1.17` tag. 

    ```bash
    kind create cluster --image kindest/node:v1.17.11
    ```

    For checking the latest available version for the `kindest/node`, you can do so as follows:

    ```bash
    curl https://registry.hub.docker.com/v2/repositories/kindest/node/tags/ \
        | jq '.results[] | select(.name|test("1.17")) | .name'
    ```

    Keep in mind that there is no guarantee that at some point, after some kind release, that image will not be overwritten with a new one that is not compatible with previous version. This means that to use a specific version of Kubernetes we have to include the SHA256 of the image in the name. ie, the lowest supported `kindest` version for StackGres is the `1.16.x` family, with the following tag:

    ```bash
    kind create cluster --image kindest/node:v1.16.9@sha256:7175872357bc85847ec4b1aba46ed1d12fa054c83ac7a8a11f5c268957fd5765
    ```


3. Install NFS utility for backups:

    This is required in order to store PostgreSQL backups in a shared NFS disk.

    ```bash
    docker exec -ti kind-control-plane sh -c 'DEBIAN_FRONTEND=noninteractive apt-get update -y -qq < /dev/null > /dev/null'
    docker exec -ti kind-control-plane sh -c 'DEBIAN_FRONTEND=noninteractive apt-get install -y -qq nfs-common < /dev/null > /dev/null'
    ```
