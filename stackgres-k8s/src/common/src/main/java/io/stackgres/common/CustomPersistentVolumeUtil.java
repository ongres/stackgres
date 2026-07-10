/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stackgres.common.crd.CustomVolumeMount;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;

/**
 * Utilities shared by the operator and the cluster controller to deal with
 * {@code SGCluster.spec.pods.customPersistentVolumes}.
 *
 * <p>Each custom persistent volume is generated as a StatefulSet volume claim template named
 * {@code custom-<name>} (see {@link StackGresVolume#CUSTOM}) so that users reference them in
 * {@code SGCluster.spec.pods.customVolumeMounts} exactly like custom volumes. Volumes that are
 * part of the coherent data set (or define I/O limits) are also mounted, at the root of the
 * PersistentVolumeClaim without any subPath, in the cluster controller container under
 * {@link ClusterPath#CUSTOM_PERSISTENT_VOLUMES_PATH}.</p>
 */
public interface CustomPersistentVolumeUtil {

  static List<StackGresClusterPodsCustomPersistentVolume> getCustomPersistentVolumes(
      StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomPersistentVolumes)
        .orElse(List.of());
  }

  static Optional<String> getWalPath(StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPostgres)
        .map(StackGresClusterConfigurationsPostgres::getWalPath);
  }

  static String volumeName(StackGresClusterPodsCustomPersistentVolume customPersistentVolume) {
    return volumeName(customPersistentVolume.getName());
  }

  static String volumeName(String name) {
    return StackGresVolume.CUSTOM.getName(name);
  }

  static Path controllerMountPath(
      StackGresClusterPodsCustomPersistentVolume customPersistentVolume) {
    return controllerMountPath(customPersistentVolume.getName());
  }

  static Path controllerMountPath(String name) {
    return Paths.get(ClusterPath.CUSTOM_PERSISTENT_VOLUMES_PATH.path()).resolve(name);
  }

  static boolean isCoherentData(
      StackGresClusterPodsCustomPersistentVolume customPersistentVolume) {
    return Boolean.TRUE.equals(customPersistentVolume.getCoherentData());
  }

  static boolean isAllowCoherentDataRemoval(
      StackGresClusterPodsCustomPersistentVolume customPersistentVolume) {
    return Boolean.TRUE.equals(customPersistentVolume.getAllowCoherentDataRemoval());
  }

  /**
   * Return the mounts of the volume generated for the custom persistent volume with the given
   * name declared for the patroni container in
   * {@code SGCluster.spec.pods.customVolumeMounts}.
   */
  static List<CustomVolumeMount> patroniMountsOf(StackGresCluster cluster, String name) {
    final String volumeName = volumeName(name);
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomVolumeMounts)
        .map(customVolumeMounts -> customVolumeMounts
            .get(StackGresContainer.PATRONI.getName()))
        .stream()
        .flatMap(List::stream)
        .filter(mount -> volumeName.equals(mount.getName()))
        .toList();
  }

  /**
   * Translate an absolute path as seen by the patroni container to the equivalent path as seen
   * by the cluster controller container.
   *
   * <p>Paths under the data volume ({@link ClusterPath#PG_BASE_PATH}) are returned unchanged
   * since the data volume is mounted at the same path in both containers. Paths under a mount
   * of a custom persistent volume are translated to the corresponding path under the mount of
   * the PersistentVolumeClaim root in the cluster controller container, taking the mount
   * subPath into account. Returns empty when the path can not be translated (it does not
   * belong to the data volume nor to any custom persistent volume mount of the patroni
   * container).</p>
   */
  static Optional<Path> translatePatroniPathToController(
      StackGresCluster cluster, String patroniPath) {
    final Path path = Paths.get(patroniPath).normalize();
    if (path.startsWith(Paths.get(ClusterPath.PG_BASE_PATH.path()))) {
      return Optional.of(path);
    }
    return getCustomPersistentVolumes(cluster)
        .stream()
        .flatMap(customPersistentVolume -> patroniMountsOf(
            cluster, customPersistentVolume.getName())
            .stream()
            .map(mount -> Map.entry(customPersistentVolume, mount)))
        .filter(volumeAndMount -> path.startsWith(
            Paths.get(volumeAndMount.getValue().getMountPath())))
        .findFirst()
        .map(volumeAndMount -> {
          final Path mountPath = Paths.get(volumeAndMount.getValue().getMountPath());
          final Path volumeRoot = controllerMountPath(volumeAndMount.getKey());
          final Path subPathRelativePath = Optional
              .ofNullable(volumeAndMount.getValue().getSubPath())
              .map(volumeRoot::resolve)
              .orElse(volumeRoot);
          return subPathRelativePath.resolve(mountPath.relativize(path));
        });
  }

}
