# StackGres Helm Operator Bundle

This module build the bundle and the images to be used with OLM.

See also https://sdk.operatorframework.io/docs/overview/

# Build

To create the bundle in the bundle folder:

```
make bundle
```

# Build for OpenShift

To create the bundle for openshift in the bundle folder:

```
make bundle-openshift
```

# Build images

To create the operator bundle image

```
make bundle-build
```

# Deploy images

The version to deploy is never inferred from the checked out tree: every script
below requires it to be set explicitly in the `STACKGRES_VERSION` environment
variable.

## OperatorHub

To deploy to [OperatorHub operators reporitory](https://github.com/k8s-operatorhub/community-operators):

```
STACKGRES_VERSION="<version>" FORK_GIT_URL="<URL of fork for https://github.com/k8s-operatorhub/community-operators>" sh deploy-to-operatorhub.sh
```

### Test OperatorHub pipeline locally

See `Vagrantfile.operatorhub-test-suite` file notes.

## Red Hat Marketplace

To deploy to [Red Had Marketplace operators reporitory](https://github.com/redhat-openshift-ecosystem/redhat-marketplace-operators):

```
STACKGRES_VERSION="<version>" FORK_GIT_URL="<URL of fork for https://github.com/redhat-openshift-ecosystem/redhat-marketplace-operators>" sh deploy-to-red-hat-marketplace.sh
```

### Test RedHat Marketplace pipeline locally

Use `../openshift-certification/start-openshift-operator-certification-pipeline.sh` script (required [`crc`](https://github.com/crc-org/crc) to be installed).

## Red Hat Certified

To deploy to [Red Had Certified operators reporitory](https://github.com/redhat-openshift-ecosystem/certified-operators):

```
STACKGRES_VERSION="<version>" FORK_GIT_URL="<URL of fork for https://github.com/redhat-openshift-ecosystem/certified-operators>" sh deploy-to-red-hat-certified.sh
```

### Test RedHat Certified pipeline locally

Use `../openshift-certification/start-openshift-operator-certification-pipeline.sh` script (required [`crc`](https://github.com/crc-org/crc) to be installed).

## Red Hat Community

To deploy to [Red Had Community operators reporitory](https://github.com/redhat-openshift-ecosystem/community-operators-prod):

```
STACKGRES_VERSION="<version>" FORK_GIT_URL="<URL of fork for https://github.com/redhat-openshift-ecosystem/community-operators-prod>" sh deploy-to-red-hat-community.sh
```

# Update the file-based catalogs

The three Red Hat repositories serve the operator through a [file-based catalog
(FBC)](https://redhat-openshift-ecosystem.github.io/operator-pipelines/users/fbc_workflow/):
`operators/<operator>/catalog-templates/v4.*.yaml` holds the update graph of
every channel, and `catalogs/v4.*/<operator>/catalog.yaml` is rendered from it.

Every channel keeps only the latest version of each minor: every other patch,
rc, beta or alpha of that minor is removed from the catalog and is in the
`skips` of the kept version, and the kept versions are chained by `replaces` in
semver order. For example:

```
1.18.10  replaces 1.17.4   skips 1.18.0 … 1.18.9
1.19.3   replaces 1.18.10  skips 1.19.0 … 1.19.2
```

A cluster running a version that is no longer in the catalog is still upgraded
through those `skips`, while the catalogs stay small (see below).

Normally `deploy.sh` generates a `release-config.yaml` next to the bundle and
Red Hat's auto-release opens a follow-up PR that adds the version to the
catalogs, replacing the latest version of the previous minor and skipping the
lower versions of its own. That pipeline can only *append* an entry to a
channel, so it only works for a version that becomes the new channel head, and
it leaves the versions the new one skips in the catalog: once its PR is merged
remove them with `skip-up-to-latest-minor-in-*.sh` (see below).

## Publish a version

A version on a minor older than the latest one in the catalog (for example
1.18.9 released when 1.19 is already out) belongs in the *middle* of the update
graph. Appending it would leave the channel with two heads and fail catalog
validation, so `deploy.sh` skips `release-config.yaml` for such a version and
it has to be published explicitly:

```
STACKGRES_VERSION="<version>" FORK_GIT_URL="<URL of fork>" sh publish-to-red-hat-certified.sh
```

The same script exists for the other two catalogs
(`publish-to-red-hat-marketplace.sh`, `publish-to-red-hat-community.sh`). It
places the version in semver order in every channel it belongs to, replacing
the lower versions of its minor (which it skips), re-points the entry of the
next minor to it, renders and validates the catalogs, and commits the result.

`STACKGRES_VERSION` can also be a range `<from>..<to>` of the versions
published in the repository. For each minor of the range only the latest
version is added to a channel, and the others of that minor (patch, rc, beta or
alpha releases belonging to the channel) are added to its `skips`, so that a
cluster running any of them is first upgraded to the latest patch of its minor
and then to the next minor. A version lower than one of its minor already in
the catalog is only added to its `skips`. For example, to give an upgrade path
to clusters on 1.17.x and 1.18.0, that are neither in the catalog nor skipped:

```
STACKGRES_VERSION="1.17.0..1.18.10" FORK_GIT_URL="<URL of fork>" sh publish-to-red-hat-certified.sh
```

Run it only after the bundle PR has been merged and Red Hat has published the
bundle image: the catalog references bundles by digest, and the digest does not
exist before then.

## Remove a version

Catalogs for OpenShift below 4.17 must store each bundle as `olm.bundle.object`,
with the CSV and every CRD inline, which for StackGres is a few MB per bundle.
Those catalogs therefore grow past GitHub's file size limits over time and old
versions have to be pruned:

```
STACKGRES_VERSION="<version>" FORK_GIT_URL="<URL of fork>" sh remove-from-red-hat-certified.sh
```

The upgrade graph is kept intact: whatever replaced the removed version now
replaces its predecessor, and the removed version is added to the `skips` of its
successor, so a cluster still running it is offered an upgrade. Only fresh
installs of that exact version become impossible.

## Skip up to the latest minor

To leave only the latest version of each minor in every channel, after Red
Hat's auto-release added a new version or for a catalog that predates this
policy:

```
FORK_GIT_URL="<URL of fork>" sh skip-up-to-latest-minor-in-red-hat-certified.sh
```

The same script exists for the other two catalogs
(`skip-up-to-latest-minor-in-red-hat-marketplace.sh`,
`skip-up-to-latest-minor-in-red-hat-community.sh`). It renders and validates
the catalogs and commits the result, or does nothing when the catalog already
has that shape. `publish-to-*.sh` and `remove-from-*.sh` do the same on every
run.

# Show the published versions

To generate `VERSIONS-<repository>.md`, with a mermaid graph of the update graph
of every channel currently published in a repository:

```
sh versions-of-red-hat-certified.sh
```

The same script exists for the other repositories (`versions-of-operatorhub.sh`,
`versions-of-red-hat-marketplace.sh`, `versions-of-red-hat-community.sh`). It
pulls the latest upstream repository first and does not need a fork. For the
file-based catalogs the graphs of all the OCP catalogs are merged, labeling any
version or edge that is not in every one of them with the catalogs it is in.
