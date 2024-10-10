---
title: Images Repository Cache
weight: 7
url: /administration/configuration/images-repository-cache
description: How to cache the StackGres images repository (the docir REST API) and mirror its images locally.
showToc: true
---

The SGClusters with `spec.configurations.registry.enabled` set to `true` run the images served by the
StackGres images repository (the docir REST API at `https://sgcr.dev` by default, see the `repository`
section of the SGConfig). The operator resolves the catalog (base images, platforms, tshirt sizes,
flavors, versions, extensions and addons) and the image of every container from that REST API.

The images repository cache stores that content locally and serves it to the operator, providing:

- **Reduced external dependencies**: the catalog and the resolved images are served from local files
- **Faster resolutions**: the images requested by the operator are resolved once
- **A local image registry**: the resolved images are copied into an image registry hosted by the cache
- **Offline capability**: an image with the whole catalog and the preloaded images can be built for
  air-gapped systems

> **Note**: The images repository cache is an experimental feature that can only be enabled through the
> Helm chart.

## How It Works

The cache is a StatefulSet (`<release name>-docir-cache`) with three containers:

1. **http**: an nginx that serves the requests of the operator. The catalog is served from the files
   stored by the controller (the request URI is the name of the file). The resolution of an image (the
   `image-url` endpoint) is a POST request that carries, as query, the key that describes every layer
   of the image: the response stored by the controller under that key is served and, when it is not
   stored yet, the request is proxied to the repository and the response cached. The `/v2/` path is
   proxied to the local image registry.
2. **registry**: a plain image registry (`docker.io/library/registry:2` by default) where the controller
   copies the resolved images.
3. **controller**: refreshes the whole catalog every `refreshInterval` (1 hour by default), preloads the
   images listed in `preloadedImages` and the ones requested by the operator (found in the nginx access
   log), storing the responses of the repository, and copies the resolved images into the local image
   registry.

The SGConfig `repository.url` is set by the chart to the cache Service so that the operator, the REST
API and the cluster controllers reach the repository through the cache.

## Enabling the Cache

```yaml
# values.yaml
repository:
  url: https://sgcr.dev
  cache:
    enabled: true
    persistentVolume:
      size: 20Gi
```

```bash
helm install stackgres-operator stackgres-charts/stackgres-operator -f values.yaml
```

For testing only, a host path can be used instead of a PersistentVolume with `repository.cache.hostPath`.

## Preloading Images

The images to preload use the syntax of the cache key of the operator, where every layer is
`<name>[@<version>[@<revision>]]` and omitted versions and revisions are resolved to the latest of the
catalog:

```
tshirt-size=full&base=<base>[@<version>[@<revision>]]&flavors=<flavor>@<version>[@<revision>][&extensions=<name>[@<version>[@<revision>]]]...[&addons=<name>[@<version>[@<revision>]]]...
```

For example, to preload the image of the Postgres containers of a PostgreSQL 16.15 SGCluster with the
extensions installed by default and the images of its sidecars:

```yaml
repository:
  cache:
    enabled: true
    preloadedImages:
    - tshirt-size=full&base=debian&flavors=postgres@16.15&extensions=pg_stat_statements&extensions=dblink&extensions=auto_explain&addons=patroni&addons=wal-g&addons=hdrhistogram
    - tshirt-size=full&base=debian&flavors=postgres@16.15&addons=pgbouncer
    - tshirt-size=full&base=debian&flavors=postgres@16.15&addons=postgres-exporter
    - tshirt-size=full&base=debian&flavors=postgres@16.15&addons=kubectl
```

Only the `full` tshirt size is used by the operator so preloading other tshirt sizes is not needed.
The images requested by the operator that are not preloaded are resolved through the cache on demand
and stored (and copied into the local registry) afterwards by the controller.

## Pulling the Images From the Cache

When `repository.cache.pullImages` is `true` (the default) the resolved images are copied into the
local image registry hosted by the cache. To make the Pods pull the images from it instead of the
repository set `repository.cache.imageRegistry` to the address (`<host>[:<port>]`) through which the
nodes reach the cache Service (for example a NodePort or a mirror configured in the container runtime):
the URLs of the resolved images are rewritten to that address. The container runtime of the nodes must
allow the plain HTTP registry.

## Air-gapped Systems

The `build-offline-docir.sh` script of the Helm chart sources builds an image with the whole catalog and
the preloaded image resolutions:

```bash
sh stackgres-k8s/install/helm/build-offline-docir.sh 1.20 \
  'tshirt-size=full&base=debian&flavors=postgres@16.15&extensions=pg_stat_statements&extensions=dblink&extensions=auto_explain&addons=patroni&addons=wal-g&addons=hdrhistogram' \
  'tshirt-size=full&base=debian&flavors=postgres@16.15&addons=pgbouncer'
```

The first parameter is the `<major>.<minor>` version of the operator (the catalog is requested for it),
the following ones are the images to preload (or a file with one image per line). The image
(`stackgres-offline-docir` by default, set `IMAGE` to change it) has to be pushed to a registry
reachable by the air-gapped system and used as the `http` container image of the cache StatefulSet
with `repository.cache.offline` set to `true`, so that the cache never reaches the repository. The
images themselves have to be mirrored in a registry of the air-gapped system and
`repository.cache.imageRegistry` set to its address.
