---
title: Pre-requisites
weight: 1
---

# Pre-requisites

* Java OpenJDK 1.8 or higher
* GraalVM 19.2.1 (Optional. Required for native image tests)
* Maven 3.6.2 ot higher
* Docker 19.03.5 or higher
* Kuberenetes [kind](https://github.com/kubernetes-sigs/kind)
* Podman
* Buildah

# Setting up the development environment

## Ubuntu

* Install build-essential, libz-dev, zliblg-dev by running: sudo apt-get install build-essential libz-dev zlib1g-dev
* Add ``'docker.io'`` to the podman's list of search registries. 
  To do that, edit the file /etc/containers/registries.conf look for '[registries.search]', in the line it shuold be the 
  list of the search registries simply add ``'docker.io'`` so it should look like this: `registries = ['docker.io']`
* Go to the directory stackgres/stackgres-k8s/src and run: mvn clean install



