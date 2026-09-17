/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;
import org.jooq.lambda.Seq;

/**
 * The layers of an image requested to the docir REST API: the base image and the Postgres flavor
 * pinned in the status of the cluster, the extensions and the addons (pinned in the status of the
 * cluster too). All the layers must be built on the same base image (name, major and minor).
 */
public class DocirImageIndex {

  private final String imageName;
  private final DocirFlavorMetadata flavor;
  private final Set<DocirExtensionMetadata> extensions;
  private final List<StackGresClusterStatusAddon> addons;
  private final DocirFlavorMetadata oldFlavor;
  private final Set<DocirExtensionMetadata> oldExtensions;
  private final boolean isPartial;

  /**
   * The image of the patroni container: base, flavor, extensions and the
   * {@link DocirUtil#POSTGRES_IMAGE_ADDONS}. Named {@code patroni-<patroni version>-<patroni
   * revision>-<flavor>-<postgres version>}.
   */
  public static DocirImageIndex fromCluster(
      StackGresContext context,
      StackGresCluster cluster) {
    return fromCluster(context, cluster, DocirUtil.PATRONI_ADDON);
  }

  private static DocirImageIndex fromCluster(
      StackGresContext context,
      StackGresCluster cluster,
      String imageNamePrefix) {
    final DocirFlavorMetadata flavor = flavorOf(cluster);
    return new DocirImageIndex(
        imageNameOf(
            imageNamePrefix, flavor.getPatroniVersion(), patroniRevisionOf(flavor),
            flavor.getFlavor().getName(), flavor.getFlavor().getVersion()),
        flavor,
        extensionsOf(context, cluster),
        DocirUtil.getStatusAddons(cluster, DocirUtil.POSTGRES_IMAGE_ADDONS),
        null,
        null,
        false);
  }

  /**
   * The image of the postgres-util container: the same layers of the image of the patroni container
   * (see {@link #fromCluster(StackGresContext, StackGresCluster)}) requested under the name
   * {@code postgres-util-<patroni version>-<patroni revision>-<flavor>-<postgres version>}.
   */
  public static DocirImageIndex fromClusterForPostgresUtil(
      StackGresContext context,
      StackGresCluster cluster) {
    return fromCluster(context, cluster, DocirUtil.POSTGRES_UTIL_IMAGE);
  }

  /**
   * The partial image (without base and flavor layers) of a single extension, used to install the
   * extension at runtime. Named {@code <flavor>-<postgres version>-<extension>-<extension
   * version>-<extension revision>}.
   */
  public static DocirImageIndex fromExtension(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresClusterInstalledExtension extension) {
    final DocirFlavorMetadata flavor = flavorOf(cluster, null, null, null);
    return new DocirImageIndex(
        imageNameOf(
            flavor.getFlavor().getName(), flavor.getFlavor().getVersion(),
            extension.getName(), extension.getVersion(), extension.getBuild()),
        flavor,
        Set.of(new DocirExtensionMetadata(extension)),
        List.of(),
        null,
        null,
        true);
  }

  /**
   * The image of the patroni container during a major version upgrade that includes the layers of
   * both the target and the previous Postgres flavor version (and their extensions). Named
   * {@code patroni-<patroni version>-<patroni revision>-<flavor>-<previous postgres
   * version>-to-<postgres version>}.
   */
  public static DocirImageIndex fromClusters(
      StackGresContext context,
      StackGresCluster cluster,
      StackGresCluster oldCluster) {
    final DocirFlavorMetadata flavor = flavorOf(cluster);
    final DocirFlavorMetadata oldFlavor = flavorOf(oldCluster);
    return new DocirImageIndex(
        imageNameOf(
            DocirUtil.PATRONI_ADDON, flavor.getPatroniVersion(), patroniRevisionOf(flavor),
            flavor.getFlavor().getName(), oldFlavor.getFlavor().getVersion(),
            "to", flavor.getFlavor().getVersion()),
        flavor,
        extensionsOf(context, cluster),
        DocirUtil.getStatusAddons(cluster, DocirUtil.POSTGRES_IMAGE_ADDONS),
        oldFlavor,
        oldExtensionsOf(context, oldCluster),
        false);
  }

  /**
   * The image of a sidecar (or Job) container: base, flavor and a single addon. Named
   * {@code <addon>-<addon version>-<addon revision>}.
   */
  public static DocirImageIndex fromAddon(
      StackGresContext context,
      StackGresCluster cluster,
      String addon) {
    final StackGresClusterStatusAddon statusAddon = cluster.getStatus().findAddon(addon)
        .orElseThrow(() -> new IllegalStateException(
            "Addon " + addon + " is not set in the status of SGCluster "
            + cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName()));
    return new DocirImageIndex(
        imageNameOf(statusAddon.getName(), statusAddon.getVersion(), statusAddon.getRevision()),
        flavorOf(cluster),
        Set.of(),
        List.of(statusAddon),
        null,
        null,
        false);
  }

  private DocirImageIndex(
      String imageName,
      DocirFlavorMetadata flavor,
      Set<DocirExtensionMetadata> extensions,
      List<StackGresClusterStatusAddon> addons,
      DocirFlavorMetadata oldFlavor,
      Set<DocirExtensionMetadata> oldExtensions,
      boolean isPartial) {
    this.imageName = imageName;
    this.flavor = flavor;
    this.extensions = extensions;
    this.addons = addons;
    this.oldFlavor = oldFlavor;
    this.oldExtensions = oldExtensions;
    this.isPartial = isPartial;
  }

  private static String patroniRevisionOf(DocirFlavorMetadata flavor) {
    return Optional.ofNullable(flavor.getPatroniRevision())
        .map(DocirRevision::getRevision)
        .orElse(null);
  }

  /**
   * Join the non null tokens with {@code -} into a valid OCI repository name (lower case letters,
   * digits and the separators {@code .}, {@code _} and {@code -}).
   */
  static String imageNameOf(String... tokens) {
    return Seq.of(tokens)
        .filter(Objects::nonNull)
        .map(token -> token.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-"))
        .filter(token -> !token.isEmpty())
        .toString("-");
  }

  /**
   * The base image (name, major and minor version and revision) pinned in the status of the
   * cluster.
   */
  public static DocirBase baseOf(StackGresCluster cluster) {
    final StackGresClusterStatus clusterStatus = cluster.getStatus();
    final var baseMajorMinor = DocirUtil.getBaseMajorMinor(clusterStatus.getBaseVersion());
    final DocirBase base = new DocirBase();
    base.setName(clusterStatus.getBase());
    base.setMajor(baseMajorMinor.major());
    base.setMinor(baseMajorMinor.minor());
    base.setRepository(clusterStatus.getRepository());
    base.setRevision(clusterStatus.getBaseRevision());
    return base;
  }

  private static DocirFlavorMetadata flavorOf(StackGresCluster cluster) {
    final StackGresClusterStatus clusterStatus = cluster.getStatus();
    return flavorOf(
        cluster,
        clusterStatus.findAddon(DocirUtil.PATRONI_ADDON).orElse(null),
        clusterStatus.findAddon(DocirUtil.WALG_ADDON).orElse(null),
        clusterStatus.findAddon(DocirUtil.HDRHISTOGRAM_ADDON).orElse(null));
  }

  private static DocirFlavorMetadata flavorOf(
      StackGresCluster cluster,
      StackGresClusterStatusAddon patroni,
      StackGresClusterStatusAddon walg,
      StackGresClusterStatusAddon hdrhistogram) {
    final StackGresClusterStatus clusterStatus = cluster.getStatus();
    // The version in the spec may be "latest" or a major version: in such case the version
    // resolved in the status is used.
    final var postgresMajorMinor = DocirUtil.getPostgresMajorMinor(
        Optional.ofNullable(cluster.getSpec().getPostgres().getVersion())
        .filter(version -> version.indexOf('.') > 0)
        .orElseGet(clusterStatus::getPostgresVersion));
    final var baseMajorMinor = DocirUtil.getBaseMajorMinor(clusterStatus.getBaseVersion());
    return new DocirFlavorMetadata(
        DocirUtil.getFlavorName(cluster),
        postgresMajorMinor.major(),
        postgresMajorMinor.minor(),
        clusterStatus.getRevision(),
        clusterStatus.getBase(),
        baseMajorMinor.major(),
        baseMajorMinor.minor(),
        clusterStatus.getBaseRevision(),
        Optional.ofNullable(patroni).map(StackGresClusterStatusAddon::getVersion).orElse(null),
        Optional.ofNullable(patroni).map(StackGresClusterStatusAddon::getRevision).orElse(null),
        Optional.ofNullable(walg).map(StackGresClusterStatusAddon::getVersion).orElse(null),
        Optional.ofNullable(walg).map(StackGresClusterStatusAddon::getRevision).orElse(null),
        Optional.ofNullable(hdrhistogram).map(StackGresClusterStatusAddon::getVersion).orElse(null),
        Optional.ofNullable(hdrhistogram).map(StackGresClusterStatusAddon::getRevision).orElse(null),
        clusterStatus.getRepository());
  }

  /**
   * The extensions requested in {@code .spec.postgres.extensions} as resolved by the operator in
   * {@code .status.extensions} (see
   * {@code io.stackgres.operator.conciliation.cluster.context.ClusterExtensionsContextAppender}),
   * the same list the cluster controller installs.
   */
  private static Set<DocirExtensionMetadata> extensionsOf(
      StackGresContext context,
      StackGresCluster cluster) {
    return extensionsOf(
        context,
        cluster,
        Seq.seq(Optional.ofNullable(cluster.getStatus().getExtensions()))
            .flatMap(List::stream)
            .map(DocirExtensionMetadata::new));
  }

  private static Set<DocirExtensionMetadata> extensionsOf(
      StackGresContext context,
      StackGresCluster cluster,
      Seq<DocirExtensionMetadata> extensions) {
    final StackGresClusterStatus clusterStatus = cluster.getStatus();
    return extensions
        .append(StackGresUtil.getDefaultClusterExtensions(context, cluster)
            .stream()
            .map(extension -> context
                .getMetadataManager()
                .findExtensionCandidateAnyVersion(
                    context,
                    cluster,
                    new StackGresClusterExtensionBuilder()
                    .withName(extension.extensionName())
                    .withRepository(clusterStatus.getRepository())
                    .withVersion(extension.extensionVersion().orElse(null))
                    .build(),
                    false)
                .orElseThrow(() -> new RuntimeException(
                    "Default extension " + extension + " not found"))))
        .grouped(Function.<DocirExtensionMetadata>identity()
            .andThen(DocirExtensionMetadata::getExtension)
            .andThen(DocirExtension::getName))
        .map(group -> group.v2().sorted(Comparator.reverseOrder()).findFirst().get())
        .collect(Collectors.toSet());
  }

  /**
   * The extensions of the cluster before a major version upgrade
   * ({@code .status.dbOps.majorVersionUpgrade.sourcePostgresExtensions}, set by
   * {@code io.stackgres.operator.conciliation.factory.cluster.MajorVersionUpgrade} in
   * {@code .spec.postgres.extensions} of {@code oldCluster}) resolved for the previous Postgres
   * version. The status of {@code oldCluster} is the one of the cluster being upgraded, that is
   * the extensions resolved for the target Postgres version, so it can not be used here.
   */
  private static Set<DocirExtensionMetadata> oldExtensionsOf(
      StackGresContext context,
      StackGresCluster oldCluster) {
    return extensionsOf(
        context,
        oldCluster,
        Seq.seq(Optional.ofNullable(oldCluster.getSpec().getPostgres().getExtensions()))
            .flatMap(List::stream)
            .map(extension -> oldExtensionOf(context, oldCluster, extension)));
  }

  /**
   * The extension of the cluster before a major version upgrade resolved for the previous Postgres
   * version. The version of an extension is not necessarily set in
   * {@code .spec.postgres.extensions} (the operator resolves it in {@code .status.extensions},
   * that here holds the extensions resolved for the target Postgres version), in such case the
   * latest version available for the previous Postgres version is used.
   */
  private static DocirExtensionMetadata oldExtensionOf(
      StackGresContext context,
      StackGresCluster oldCluster,
      StackGresClusterExtension extension) {
    return context.getMetadataManager()
        .findExtensionCandidateSameMajorBuild(context, oldCluster, extension, false)
        .or(() -> Optional.of(extension)
            .filter(anExtension -> anExtension.getVersion() == null)
            .flatMap(anExtension -> context.getMetadataManager()
                .findExtensionCandidateAnyVersion(context, oldCluster, anExtension, false)))
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find candidate version of extension "
                + DocirUtil.getDescription(context, oldCluster, extension, false)));
  }

  /**
   * The name of the repository requested to docir for the image, see
   * {@link io.stackgres.common.docir.model.ImageUrlRequest#name()}.
   */
  public String getImageName() {
    return imageName;
  }

  public DocirFlavorMetadata getFlavor() {
    return flavor;
  }

  public Set<DocirExtensionMetadata> getExtensions() {
    return extensions;
  }

  public List<StackGresClusterStatusAddon> getAddons() {
    return addons;
  }

  public DocirFlavorMetadata getOldFlavor() {
    return oldFlavor;
  }

  public Set<DocirExtensionMetadata> getOldExtensions() {
    return oldExtensions;
  }

  public boolean isPartial() {
    return isPartial;
  }

  @Override
  public int hashCode() {
    return Objects.hash(addons, extensions, flavor, isPartial, oldExtensions, oldFlavor);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirImageIndex)) {
      return false;
    }
    DocirImageIndex other = (DocirImageIndex) obj;
    return Objects.equals(addons, other.addons)
        && Objects.equals(extensions, other.extensions) && Objects.equals(flavor, other.flavor)
        && isPartial == other.isPartial && Objects.equals(oldExtensions, other.oldExtensions)
        && Objects.equals(oldFlavor, other.oldFlavor);
  }

}
