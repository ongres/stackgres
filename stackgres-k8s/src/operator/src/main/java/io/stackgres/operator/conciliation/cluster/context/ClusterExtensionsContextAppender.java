/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.common.extension.StackGresExtensionVersion;
import io.stackgres.operator.conciliation.AbstractExtensionsContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterExtensionsContextAppender extends AbstractExtensionsContextAppender<StackGresCluster, Builder> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ClusterExtensionsContextAppender.class);

  private final StackGresContext context;
  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ClusterExtensionsContextAppender(
      StackGresContext context,
      ExtensionMetadataManager extensionMetadataManager) {
    this.context = context;
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected StackGresContext getContext() {
    return context;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresCluster inputContext, String version, String buildVersion) {
    return Optional.of(inputContext)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresCluster inputContext, String version, String buildVersion) {
    return StackGresUtil.getDefaultClusterExtensions(context, inputContext);
  }

  @Override
  protected void setToInstallExtensions(StackGresCluster resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresClusterStatus());
    }
    final List<StackGresClusterExtension> specExtensions = Optional.of(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
    toInstallExtensions.forEach(
        installedExtension -> setLatestVersion(resource, specExtensions, installedExtension));
    resource.getStatus().setExtensions(toInstallExtensions);
  }

  /**
   * Advertise the latest available version of the extension when it does not match the version
   * requested in {@code .spec.postgres.extensions[]} for the entry with the same name, publisher and
   * repository, so {@code ClusterStatusManager} can build the {@code ComponentsUpdated} condition
   * straight from the status. Failures querying the extensions repository are logged and the
   * extension is left without a latest version.
   */
  private void setLatestVersion(
      StackGresCluster cluster,
      List<StackGresClusterExtension> specExtensions,
      StackGresClusterInstalledExtension installedExtension) {
    final Optional<String> specVersion = specExtensions.stream()
        .filter(specExtension -> Objects.equals(
            specExtension.getName(), installedExtension.getName())
            && Objects.equals(
                specExtension.getPublisherOrDefault(), installedExtension.getPublisher())
            && (specExtension.getRepository() == null
                || Objects.equals(
                    specExtension.getRepository(), installedExtension.getRepository())))
        .map(StackGresClusterExtension::getVersion)
        .filter(Objects::nonNull)
        .findFirst();
    if (specVersion.isEmpty()) {
      return;
    }
    try {
      final StackGresClusterExtension extension = new StackGresClusterExtension();
      extension.setName(installedExtension.getName());
      extension.setPublisher(installedExtension.getPublisher());
      extension.setRepository(installedExtension.getRepository());
      extensionMetadataManager
          .findExtensionCandidateSameMajorBuild(cluster, extension, false)
          .map(StackGresExtensionMetadata::getVersion)
          .map(StackGresExtensionVersion::getVersion)
          .filter(latest -> !Objects.equals(latest, specVersion.get()))
          .ifPresent(installedExtension::setLatest);
    } catch (RuntimeException ex) {
      LOGGER.warn("Unable to check the latest version of extension {} of cluster {}/{}",
          installedExtension.getName(), cluster.getMetadata().getNamespace(),
          cluster.getMetadata().getName(), ex);
    }
  }

}
