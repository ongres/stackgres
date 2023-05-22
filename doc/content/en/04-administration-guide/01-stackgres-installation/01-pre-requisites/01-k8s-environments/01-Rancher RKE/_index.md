---
title: Rancher RKE
weight: 1
url: install/prerequisites/k8s/rke
description: Rancher Kubernetes Engine (RKE) is a CNCF-certified Kubernetes distribution that runs entirely within Docker containers.
---

Rancher Kubernetes Engine (RKE) is a CNCF-certified Kubernetes distribution that runs entirely within Docker containers. It solves the common frustration of installation complexity with Kubernetes by removing most host dependencies and presenting a stable path for deployment, upgrades, and rollbacks.

> StackGres is not actively tested with RKE, if you find any problem, please [open an issue](https://gitlab.com/ongresinc/stackgres/-/issues/new).

By default, RKE looks for a file called `cluster.yml`, which contains information about the remote servers and services that will run on servers.

StackGres uses self-signed certificates to enable https on the Web UI, when you are installing StackGres it creates a Certificate Signing Request to the internal CA of Kubernetes, RKE by default do not configure the parameters `--cluster-signing-cert-file` and `--cluster-signing-key-file` on the kube-controller-manager, you need to add these lines inside "service" section in your `cluster.yaml`.

```yaml
services:
  kube-controller:
    extra_args:
      cluster-signing-cert-file: /etc/kubernetes/ssl/kube-ca.pem
      cluster-signing-key-file: /etc/kubernetes/ssl/kube-ca-key.pem
```

After you’ve updated your cluster.yml, you can deploy your cluster with a simple command. This command assumes the cluster.yml file is in the same directory as where you are running the command.

```
rke up

INFO[0000] Building Kubernetes cluster
INFO[0000] [dialer] Setup tunnel for host [10.0.0.1]
INFO[0000] [network] Deploying port listener containers
INFO[0000] [network] Pulling image [alpine:latest] on host [10.0.0.1]
...
INFO[0101] Finished building Kubernetes cluster successfully
```

The last line should read "Finished building Kubernetes cluster successfully" to indicate that your cluster is ready to use. After that continue the instalation of StackGres.
