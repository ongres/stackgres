/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.Component;
import io.stackgres.common.component.Component.ComposedComponentVersion;
import io.stackgres.common.component.DocirVersionReader.DocirComposedComponentVersion;
import io.stackgres.common.component.DocirVersionReader.DocirFlavorVersion;
import io.stackgres.common.component.DocirVersionReader.DocirHdrhistogramVersion;
import io.stackgres.common.component.DocirVersionReader.DocirPatroniVersion;
import io.stackgres.common.component.DocirVersionReader.DocirWalgVersion;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;
import io.stackgres.common.docir.DocirAddonVersion;
import io.stackgres.common.docir.DocirBase;
import io.stackgres.common.docir.DocirFlavorMetadata;
import io.stackgres.common.docir.DocirImageIndex;
import io.stackgres.common.docir.DocirUtil;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.common.StackGresVersionUtil;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterPostgresVersionContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ClusterPostgresVersionContextAppender.class);

  private static final String PG_14_CREATE_CONCURRENT_INDEX_BUG =
      "Please, use PostgreSQL 14.4 since it fixes an issue"
          + " with CREATE INDEX CONCURRENTLY and REINDEX CONCURRENTLY that"
          + " could cause silent data corruption of indexes. For more info"
          + " see https://www.postgresql.org/about/news/postgresql-144-released-2470/.";
  private static final String REPLICATION_SLOTS_INVALIDATION_BUG =
      "A bug was introduced in Postgres versions 17.5, 16.9, 15.13, 14.18 and 13.21"
          + " that can invalidate logical replication slots. For more info see"
          + " https://www.postgresql.org/message-id/flat/680bdaf6-f7d1-4536-b580-05c2760c67c6%40deepbluecap.com";
  public static final Map<String, String> BUGGY_PG_VERSIONS = Map.ofEntries(
      Map.entry("14.0", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("14.1", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("14.2", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("14.3", PG_14_CREATE_CONCURRENT_INDEX_BUG),
      Map.entry("13.21", REPLICATION_SLOTS_INVALIDATION_BUG),
      Map.entry("14.18", REPLICATION_SLOTS_INVALIDATION_BUG),
      Map.entry("15.13", REPLICATION_SLOTS_INVALIDATION_BUG),
      Map.entry("16.9", REPLICATION_SLOTS_INVALIDATION_BUG),
      Map.entry("17.5", REPLICATION_SLOTS_INVALIDATION_BUG)
      );

  private final StackGresContext context;
  private final EventEmitter<StackGresCluster> eventController;
  private final ClusterPostgresConfigContextAppender clusterPostgresConfigContextAppender;
  private final ClusterDefaultBackupPathContextAppender clusterDefaultBackupPathContextAppender;
  private final ClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender;
  private final ClusterObjectStorageContextAppender clusterObjectStorageContextAppender;
  private final ClusterExtensionsContextAppender clusterExtensionsContextAppender;
  private final KubectlUtil kubectl;

  @Inject
  public ClusterPostgresVersionContextAppender(
      StackGresContext context,
      EventEmitter<StackGresCluster> eventController,
      ClusterPostgresConfigContextAppender clusterPostgresConfigContextAppender,
      ClusterDefaultBackupPathContextAppender clusterDefaultBackupPathContextAppender,
      ClusterRestoreBackupContextAppender clusterRestoreBackupContextAppender,
      ClusterObjectStorageContextAppender clusterObjectStorageContextAppender,
      ClusterExtensionsContextAppender clusterExtensionsContextAppender,
      KubectlUtil kubectl) {
    this.context = context;
    this.eventController = eventController;
    this.clusterPostgresConfigContextAppender = clusterPostgresConfigContextAppender;
    this.clusterDefaultBackupPathContextAppender = clusterDefaultBackupPathContextAppender;
    this.clusterRestoreBackupContextAppender = clusterRestoreBackupContextAppender;
    this.clusterObjectStorageContextAppender = clusterObjectStorageContextAppender;
    this.clusterExtensionsContextAppender = clusterExtensionsContextAppender;
    this.kubectl = kubectl;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    if (cluster.getStatus() == null) {
      cluster.setStatus(new StackGresClusterStatus());
    }
    final Optional<String> previousVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPostgresVersion);
    final Optional<String> previousBuildVersion = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getBuildVersion);
    final boolean isRolloutAllowed = ClusterRolloutUtil.isRolloutAllowed(cluster);
    if (isRolloutAllowed
        && (
            cluster.getMetadata().getAnnotations() == null
            || !Objects.equals(
                cluster.getMetadata().getAnnotations().get(StackGresKeys.VERSION_KEY),
                StackGresProperty.OPERATOR_VERSION.getString())
        )) {
      cluster.getMetadata().setAnnotations(
          Seq.seq(
              Optional.ofNullable(cluster.getMetadata().getAnnotations())
              .map(Map::entrySet)
              .stream()
              .flatMap(Set::stream)
              .filter(label -> !StackGresKeys.VERSION_KEY.equals(label.getKey())))
          .append(Map.entry(StackGresKeys.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString()))
          .toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    String targetPostgresVersion = Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .orElse(StackGresComponent.LATEST);

    if (!isPostgresVersionSupported(cluster, targetPostgresVersion)) {
      eventController.sendEvent(
          ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "Unsupported postgres version " + targetPostgresVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(StackGresVersionUtil.getSupportedPostgresVersions(context, cluster))
          .toString(", "),
          cluster);
    }

    Optional<String> targetVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .findVersion(context, targetPostgresVersion);

    if (targetVersion.map(BUGGY_PG_VERSIONS.keySet()::contains).orElse(false)) {
      eventController.sendEvent(
          ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "Do not use PostgreSQL " + targetVersion.get() + ". "
              + BUGGY_PG_VERSIONS.get(targetVersion.get()),
          cluster);
    }

    String postgresVersion = previousVersion
        .filter(version -> !isRolloutAllowed)
        .orElse(targetPostgresVersion);

    if (!isPostgresVersionSupported(cluster, postgresVersion)) {
      throw new IllegalArgumentException(
          "Unsupported postgres version " + postgresVersion
          + ".  Supported postgres versions are: "
          + Seq.seq(StackGresVersionUtil.getSupportedPostgresVersions(context, cluster))
          .toString(", "));
    }

    String version = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getVersion(context, postgresVersion);
    String buildVersion = getPostgresFlavorComponent(cluster)
        .get(cluster)
        .getBuildVersion(context, postgresVersion);

    if (BUGGY_PG_VERSIONS.keySet().contains(version)
        && !Objects.equals(Optional.of(version), previousVersion)) {
      throw new IllegalArgumentException(
          "Do not use PostgreSQL " + version + ". "
              + BUGGY_PG_VERSIONS.get(version));
    }

    if (previousVersion
        .filter(Predicate.not(version::equals))
        .isPresent()) {
      String majorVersion = getPostgresFlavorComponent(cluster).get(cluster)
          .getMajorVersion(context, version);
      long majorVersionIndex = getPostgresFlavorComponent(cluster)
          .get(cluster).streamOrderedMajorVersions(context)
          .zipWithIndex()
          .filter(t -> t.v1.equals(majorVersion))
          .map(Tuple2::v2)
          .findAny()
          .get();
      String previousMajorVersion = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .getMajorVersion(context, previousVersion.get());
      long previousMajorVersionIndex = getPostgresFlavorComponent(cluster)
          .get(cluster)
          .streamOrderedMajorVersions(context)
          .zipWithIndex()
          .filter(t -> t.v1.equals(previousMajorVersion))
          .map(Tuple2::v2)
          .findAny()
          .get();
      if (majorVersionIndex < previousMajorVersionIndex
          && (
              cluster.getStatus().getDbOps() == null
              || cluster.getStatus().getDbOps().getMajorVersionUpgrade() == null)) {
        eventController.sendEvent(
            ClusterEventReason.CLUSTER_MAJOR_UPGRADE,
            "To upgrade to major Postgres version " + majorVersion + ", please create an SGDbOps operation"
                + " with \"op: majorVersionUpgrade\" and set the target postgres version to " + version + ".",
            cluster);
        version = null;
      }
      if (majorVersionIndex > previousMajorVersionIndex) {
        throw new IllegalArgumentException("Can not change the major version " + majorVersion
            + " of Postgres to the previous major version " + previousMajorVersion);
      }
    }

    if (version != null && buildVersion != null) {
      cluster.getStatus().setPostgresVersion(version);
      cluster.getStatus().setBuildVersion(buildVersion);
      setLatestPostgresVersions(cluster, version);
      if (StackGresUtil.isRegistryEnabled(cluster)) {
        if (shouldResolveRegistryImages(cluster, version, previousVersion, isRolloutAllowed)) {
          resolveRegistryImages(cluster, version);
        }
        pinAvailableSidecarAddons(cluster);
      }
      clusterPostgresConfigContextAppender.appendContext(cluster, contextBuilder, version);
      clusterDefaultBackupPathContextAppender.appendContext(cluster, contextBuilder, version);
      clusterRestoreBackupContextAppender.appendContext(cluster, contextBuilder, version);
      clusterObjectStorageContextAppender.appendContext(cluster, contextBuilder, version);
      clusterExtensionsContextAppender.appendContext(cluster, contextBuilder, version,
          buildVersion, previousVersion, previousBuildVersion, cluster);
    }

    if ((version == null && previousVersion.isEmpty())
        || (buildVersion == null && previousBuildVersion.isEmpty())) {
      throw new IllegalArgumentException("Can not determine the Postgres version to use");
    }
  }

  /**
   * The layers of the images retrieved from the StackGres images registry are pinned in the status
   * (base image, Postgres flavor and addons with their revision) so that the same images are used
   * until a rollout is allowed, exactly as the Postgres version is. They are resolved again only
   * when they were never resolved, when the Postgres version changes or when a rollout is allowed.
   * Only the layers of the image of the patroni container are required to consider the images
   * resolved, the sidecar addons are pinned when available in the repository (see
   * {@link #pinAvailableSidecarAddons(StackGresCluster)}).
   */
  private boolean shouldResolveRegistryImages(
      StackGresCluster cluster,
      String version,
      Optional<String> previousVersion,
      boolean isRolloutAllowed) {
    final StackGresClusterStatus status = cluster.getStatus();
    return isRolloutAllowed
        || status.getRepository() == null
        || status.getRevision() == null
        || status.getBase() == null
        || status.getBaseVersion() == null
        || status.getBaseRevision() == null
        || !DocirUtil.hasStatusAddons(cluster, DocirUtil.POSTGRES_IMAGE_ADDONS)
        || previousVersion.filter(version::equals).isEmpty();
  }

  /**
   * Resolve the latest build of every layer of the image of the patroni container: the Postgres
   * flavor version selected for the cluster, the base image it was built on with the latest patroni,
   * wal-g and hdrhistogram built on the same base image (that is the composed version of the patroni
   * component). Any previous pin of the sidecar addons is discarded so that they are pinned again
   * by {@link #pinAvailableSidecarAddons(StackGresCluster)}.
   */
  private void resolveRegistryImages(StackGresCluster cluster, String version) {
    final Component patroniComponent = StackGresComponent.PATRONI.get(cluster);
    final Component flavorComponent = getPostgresFlavorComponent(cluster).get(cluster);
    final ComposedComponentVersion composedVersion = patroniComponent
        .findComposedVersion(
            context,
            StackGresComponent.LATEST,
            Map.of(
                StackGresComponent.WALG.get(cluster), StackGresComponent.LATEST,
                StackGresComponent.HDRHISTOGRAM.get(cluster), StackGresComponent.LATEST,
                flavorComponent, version))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Can not find the composed component for "
            + StackGresComponent.PATRONI + " version " + StackGresComponent.LATEST
            + ", " + StackGresComponent.WALG + " version " + StackGresComponent.LATEST
            + ", " + StackGresComponent.HDRHISTOGRAM + " version " + StackGresComponent.LATEST
            + ", " + flavorComponent + " version " + version));
    if (!(composedVersion instanceof DocirComposedComponentVersion)) {
      throw new RuntimeException("Wrong composed component type: type "
          + composedVersion.getClass() + " !=  type DocirComposedComponentVersion");
    }
    if (!(composedVersion.getVersion() instanceof DocirPatroniVersion docirPatroniVersion)) {
      throw new RuntimeException("Wrong composed component type: type "
          + composedVersion.getVersion() + " !=  type DocirPatroniVersion");
    }
    if (composedVersion.getSubVersions().size() < 3) {
      throw new RuntimeException("Wrong composed component type:"
          + " subversions was less than 3 (" + composedVersion.getSubVersions().size() + ")");
    }
    if (!(composedVersion.getSubVersions().get(0).v2 instanceof DocirWalgVersion docirWalgVersion)) {
      throw new RuntimeException("Wrong composed component type:"
          + " subversion at index 0 type " + Optional.ofNullable(composedVersion.getSubVersions().get(0).v2)
          .map(cv -> cv.getClass().getSimpleName()).orElse("null") + " !=  type DocirWalgVersion");
    }
    if (!(composedVersion.getSubVersions().get(1).v2
        instanceof DocirHdrhistogramVersion docirHdrhistogramVersion)) {
      throw new RuntimeException("Wrong composed component type:"
          + " subversion at index 1 type " + Optional.ofNullable(composedVersion.getSubVersions().get(1).v2)
          .map(cv -> cv.getClass().getSimpleName()).orElse("null") + " !=  type DocirHdrhistogramVersion");
    }
    if (!(composedVersion.getSubVersions().get(2).v2 instanceof DocirFlavorVersion docirFlavorVersion)) {
      throw new RuntimeException("Wrong composed component type:"
          + " subversion at index 2 type " + Optional.ofNullable(composedVersion.getSubVersions().get(2).v2)
          .map(cv -> cv.getClass().getSimpleName()).orElse("null") + " !=  type DocirFlavorVersion");
    }
    final DocirFlavorMetadata flavorMetadata = docirFlavorVersion.getFlavor();
    final DocirBase base = flavorMetadata.getBase();
    final URI repositoryUri = URI.create(flavorMetadata.getFlavor().getRepository());
    final List<StackGresClusterStatusAddon> addons = new ArrayList<>();
    addons.add(new StackGresClusterStatusAddon(
        DocirUtil.PATRONI_ADDON,
        docirPatroniVersion.getVersion(),
        String.valueOf(docirPatroniVersion.getRevision())));
    addons.add(new StackGresClusterStatusAddon(
        DocirUtil.WALG_ADDON,
        docirWalgVersion.getVersion(),
        String.valueOf(docirWalgVersion.getRevision())));
    addons.add(new StackGresClusterStatusAddon(
        DocirUtil.HDRHISTOGRAM_ADDON,
        docirHdrhistogramVersion.getVersion(),
        String.valueOf(docirHdrhistogramVersion.getRevision())));
    cluster.getStatus().setRepository(flavorMetadata.getFlavor().getRepository());
    cluster.getStatus().setRevision(String.valueOf(docirFlavorVersion.getRevision()));
    cluster.getStatus().setBase(base.getName());
    cluster.getStatus().setBaseVersion(base.getVersion());
    cluster.getStatus().setBaseRevision(flavorMetadata.getBaseRevision());
    cluster.getStatus().setAddons(addons);
  }

  /**
   * Pin the latest version of the sidecar addons (see {@link DocirUtil#SIDECAR_ADDONS}) not yet
   * pinned in the status that are built on the same base image and platform of the image of the
   * patroni container. The kubectl addon is pinned to the version nearest to the version of the
   * Kubernetes cluster where the operator is running instead of the latest one (see
   * {@link KubectlUtil#findAddonVersion(URI, DocirBase, String, String)}), since it is used by the
   * containers of the SGDbOps and SGBackup Jobs that interact with the Kubernetes API. An addon not
   * available in the repository is skipped (a sidecar that requires it will fail only if used) and
   * pinned as soon as it becomes available, existing pins are left untouched so that the images
   * already in use are not changed.
   */
  private void pinAvailableSidecarAddons(StackGresCluster cluster) {
    final List<String> missingAddons = DocirUtil.SIDECAR_ADDONS.values().stream()
        .sorted()
        .filter(addon -> cluster.getStatus().findAddon(addon).isEmpty())
        .toList();
    if (missingAddons.isEmpty()) {
      return;
    }
    final DocirBase base = DocirImageIndex.baseOf(cluster);
    final URI repositoryUri = URI.create(cluster.getStatus().getRepository());
    final String os = DocirUtil.DEFAULT_OS;
    final String arch = DocirUtil.DEFAULT_ARCH;
    final List<StackGresClusterStatusAddon> addons = new ArrayList<>(
        Optional.ofNullable(cluster.getStatus().getAddons()).orElse(List.of()));
    for (String addon : missingAddons) {
      final Optional<DocirAddonVersion> addonVersion;
      if (DocirUtil.KUBECTL_ADDON.equals(addon)) {
        addonVersion = kubectl.findAddonVersion(repositoryUri, base, os, arch);
      } else {
        addonVersion = context.getMetadataManager()
            .findLatestAddonVersion(repositoryUri, addon, base, os, arch);
      }
      if (addonVersion.isEmpty()) {
        LOGGER.debug("Addon {} built on base image {} for platform {}/{} not found in repository {},"
            + " it will not be pinned in the status of SGCluster {}.{}",
            addon, base.getBaseIdentity(), os, arch, repositoryUri,
            cluster.getMetadata().getNamespace(), cluster.getMetadata().getName());
        continue;
      }
      addons.add(new StackGresClusterStatusAddon(
          addon, addonVersion.get().getVersion(), addonVersion.get().getRevision()));
    }
    cluster.getStatus().setAddons(addons);
  }

  /**
   * Advertise in the status the latest available Postgres minor version for the major in use and,
   * when a newer major exists, the latest available version. Each field is only set when it differs
   * from the version/major in use so {@code ClusterStatusManager} can build the
   * {@code ComponentsUpdated} condition straight from the status.
   */
  private void setLatestPostgresVersions(StackGresCluster cluster, String version) {
    final Component postgres = getPostgresFlavorComponent(cluster).get(cluster);
    final String usedMajor = postgres.getMajorVersion(context, version);
    final String latestMinorForMajor = postgres.getVersion(context, usedMajor);
    final String latestMajor = postgres.getLatestMajorVersion(context);
    cluster.getStatus().setLatestPostgresMinor(
        Objects.equals(version, latestMinorForMajor) ? null : latestMinorForMajor);
    cluster.getStatus().setLatestPostgresMajor(
        Objects.equals(usedMajor, latestMajor) ? null : postgres.getLatestVersion(context));
  }

  private boolean isPostgresVersionSupported(StackGresCluster cluster, String version) {
    if (version.contains(".")) {
      return StackGresVersionUtil.getSupportedPostgresVersions(context, cluster).contains(version);
    }
    return Optional.of(getPostgresFlavorComponent(cluster).get(cluster))
        .filter(component -> component.findVersion(context, version).isPresent())
        .isPresent();
  }

}
