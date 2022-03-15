/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.component.Component;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.jetbrains.annotations.NotNull;

public final class KubectlUtil {

  private final int k8sMinorVersion;

  private KubectlUtil(KubernetesClient client) {
    int minor;
    try {
      minor = Integer.parseInt(client.getKubernetesVersion().getMinor());
    } catch (RuntimeException e) {
      // Fallback to latest image
      minor = 0;
    }
    this.k8sMinorVersion = minor;
  }

  public static KubectlUtil fromClient() {
    try (var client = new DefaultKubernetesClient()) {
      return new KubectlUtil(client);
    }
  }

  public static KubectlUtil fromClient(KubernetesClient client) {
    return new KubectlUtil(client);
  }

  public String getImageName(@NotNull StackGresVersion sgversion) {
    Component kubectl = StackGresComponent.KUBECTL.getOrThrow(sgversion);
    return kubectl.getOrderedVersions()
        .findFirst(ver -> {
          int minor = Integer.parseInt(ver.split("\\.")[1]);
          return (k8sMinorVersion >= minor - 1 && k8sMinorVersion <= minor + 1);
        })
        .map(kubectl::findImageName)
        .orElseGet(kubectl::findLatestImageName);
  }

  public String getImageName(@NotNull StackGresCluster cluster) {
    StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(cluster);
    return getImageName(stackGresVersion);
  }

  public String getImageName(@NotNull StackGresDistributedLogs distributedLogs) {
    StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(distributedLogs);
    return getImageName(stackGresVersion);
  }

}
