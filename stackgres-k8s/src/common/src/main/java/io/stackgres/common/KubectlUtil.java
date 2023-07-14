/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.EnumMap;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.component.Component;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class KubectlUtil {

  private static final Logger LOG = LoggerFactory.getLogger(KubectlUtil.class);

  private final int k8sMinorVersion;
  private final Map<StackGresVersion, String> cache;

  @Inject
  public KubectlUtil(KubernetesClient client) {
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
      final String imageName = kubectl.streamOrderedVersions()
          .filter(ver -> k8sMinorVersion != -1)
          .findFirst(ver -> {
            int minor = Integer.parseInt(ver.split("\\.")[1]);
            return (k8sMinorVersion >= minor - 1 && k8sMinorVersion <= minor + 1);
          })
          .map(kubectl::getImageName)
          .orElseGet(kubectl::getLatestImageName);
      LOG.debug("Using kubectl image: {}", imageName);
      return imageName;
    });
  }

  public String getImageName(@NotNull StackGresCluster cluster) {
    return getImageName(StackGresVersion.getStackGresVersion(cluster));
  }

  public String getImageName(@NotNull StackGresDbOps dbOps) {
    return getImageName(StackGresVersion.getStackGresVersion(dbOps));
  }

  public String getImageName(@NotNull StackGresDistributedLogs distributedLogs) {
    return getImageName(StackGresVersion.getStackGresVersion(distributedLogs));
  }

}
