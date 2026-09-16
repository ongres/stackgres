/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.net.URI;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.component.Component;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.docir.DocirAddonVersion;
import io.stackgres.common.docir.DocirBase;
import io.stackgres.common.docir.DocirUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class KubectlUtil {

  private static final Logger LOG = LoggerFactory.getLogger(KubectlUtil.class);

  private final StackGresContext context;
  private final int k8sMinorVersion;
  private final Map<StackGresVersion, String> cache;

  @Inject
  public KubectlUtil(StackGresContext context, KubernetesClient client) {
    this.context = context;
    int minor;
    try {
      VersionInfo kubernetesVersion = client.getKubernetesVersion();
      LOG.debug("Kubernetes version: {}", kubernetesVersion.getGitVersion());
      minor = Integer.parseInt(kubernetesVersion.getMinor());
    } catch (RuntimeException e) {
      // Fallback to latest image
      minor = -1;
    }
    this.k8sMinorVersion = minor;
    this.cache = new EnumMap<>(StackGresVersion.class);
  }

  public String getImageName(@NotNull StackGresVersion sgversion) {
    return cache.computeIfAbsent(sgversion, value -> {
      Component kubectl = StackGresComponent.KUBECTL.getOrThrow(sgversion);
      final String imageName = kubectl.streamOrderedVersions(context)
          .filter(ver -> k8sMinorVersion != -1)
          .findFirst(ver -> {
            int minor = Integer.parseInt(ver.split("\\.")[1]);
            return (k8sMinorVersion >= minor - 1 && k8sMinorVersion <= minor + 1);
          })
          .map(ver -> kubectl.getImageName(context, ver))
          .orElseGet(() -> kubectl.getLatestImageName(context));
      LOG.debug("Using kubectl image: {}", imageName);
      return imageName;
    });
  }

  public String getImageName(@NotNull StackGresCluster cluster) {
    if (StackGresUtil.isRegistryEnabled(cluster)) {
      return StackGresUtil.getSidecarImageName(context, cluster, StackGresComponent.KUBECTL);
    }
    return getImageName(StackGresVersion.getStackGresVersion(cluster));
  }

  public String getImageName(@NotNull StackGresDistributedLogs distributedLogs) {
    return getImageName(StackGresVersion.getStackGresVersion(distributedLogs));
  }

  public String getImageName(@NotNull StackGresShardedCluster cluster) {
    return getImageName(StackGresVersion.getStackGresVersion(cluster));
  }

  /**
   * The version of the kubectl addon of the StackGres images repository to pin in the status of the
   * cluster: among the versions built on the base image of the image of the patroni container for
   * the platform, the one whose minor version is the nearest to the minor version of the Kubernetes
   * cluster where the operator is running and, among those, the latest build. When the Kubernetes
   * version could not be detected the latest version available is used.
   */
  public Optional<DocirAddonVersion> findAddonVersion(
      @NotNull URI repositoryUri, @NotNull DocirBase base, @NotNull String os, @NotNull String arch) {
    final List<DocirAddonVersion> versions = context.getMetadataManager()
        .getAddonVersions(repositoryUri, DocirUtil.KUBECTL_ADDON, base, os, arch);
    if (k8sMinorVersion == -1) {
      return versions.stream().findFirst();
    }
    return versions.stream()
        .min(Comparator.comparing(
            (DocirAddonVersion version) -> getMinorVersionDistance(version.getVersion())));
  }

  /**
   * The distance between the minor version of kubectl and the minor version of the Kubernetes
   * cluster where the operator is running. A version that has no minor version is the farthest.
   */
  private int getMinorVersionDistance(String version) {
    try {
      return Math.abs(k8sMinorVersion - Integer.parseInt(version.split("\\.")[1]));
    } catch (RuntimeException ex) {
      LOG.debug("Can not extract the minor version of kubectl version {}", version, ex);
      return Integer.MAX_VALUE;
    }
  }

}
