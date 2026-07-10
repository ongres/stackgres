/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.crd.CustomVolumeMount;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

/**
 * Shared validation rules for {@code pods.customPersistentVolumes} and the
 * {@code configurations.postgres.walPath} of SGCluster and of the coordinator, workers and
 * workers overrides sections of SGShardedCluster.
 */
public abstract class AbstractCustomPersistentVolumesValidator {

  private static final Pattern WAL_PATH_PATTERN = Pattern.compile("^/[A-Za-z0-9._/-]+$");

  protected abstract void failValidation(String message, String... fields)
      throws ValidationFailed;

  protected void validateCustomPersistentVolumes(
      StackGresClusterPods pods,
      Optional<String> walPath,
      Optional<StackGresClusterPods> oldPods,
      Optional<String> oldWalPath,
      List<String> appliedWalPaths,
      String fieldPrefix) throws ValidationFailed {
    final String field = fieldPrefix + ".pods.customPersistentVolumes";
    final List<StackGresClusterPodsCustomPersistentVolume> customPersistentVolumes =
        CustomPersistentVolumeUtil.getCustomPersistentVolumes(pods);

    Set<String> names = new HashSet<>();
    for (var customPersistentVolume : customPersistentVolumes) {
      if (!names.add(customPersistentVolume.getName())) {
        failValidation("Custom persistent volume name \"" + customPersistentVolume.getName()
            + "\" is duplicated", field);
      }
    }
    Set<String> customVolumeNames = Optional.ofNullable(pods)
        .map(StackGresClusterPods::getCustomVolumes)
        .stream()
        .flatMap(List::stream)
        .map(customVolume -> customVolume.getName())
        .collect(Collectors.toSet());
    for (var customPersistentVolume : customPersistentVolumes) {
      if (customVolumeNames.contains(customPersistentVolume.getName())) {
        failValidation("Custom persistent volume name \"" + customPersistentVolume.getName()
            + "\" clashes with a custom volume with the same name", field);
      }
    }

    for (var customPersistentVolume : customPersistentVolumes) {
      if (CustomPersistentVolumeUtil.isCoherentData(customPersistentVolume)
          && CustomPersistentVolumeUtil
              .patroniMountsOf(pods, customPersistentVolume.getName()).isEmpty()) {
        failValidation("Custom persistent volume \"" + customPersistentVolume.getName()
            + "\" with coherentData set to true must be mounted in the patroni container"
            + " using " + fieldPrefix + ".pods.customVolumeMounts", field);
      }
    }

    if (walPath.isPresent()) {
      validateWalPath(pods, walPath.get(), fieldPrefix);
    }

    if (oldPods.isPresent() || oldWalPath.isPresent()) {
      validateUpdate(pods, customPersistentVolumes, oldPods.orElse(null),
          appliedWalPaths, field);
    }
  }

  private void validateWalPath(
      StackGresClusterPods pods, String walPath, String fieldPrefix) throws ValidationFailed {
    final String field = fieldPrefix + ".configurations.postgres.walPath";
    if (!WAL_PATH_PATTERN.matcher(walPath).matches()) {
      failValidation("walPath must be an absolute path containing only alphanumeric"
          + " characters and the characters \"-\", \"_\", \".\" and \"/\"", field);
      return;
    }
    final Path path = Paths.get(walPath);
    if (!path.normalize().equals(path)) {
      failValidation("walPath must be a normalized path", field);
      return;
    }
    if (path.startsWith(Paths.get(ClusterPath.PG_DATA_PATH.path()))) {
      failValidation("walPath must not be under the PostgreSQL data directory ("
          + ClusterPath.PG_DATA_PATH.path() + ")", field);
      return;
    }
    if (path.equals(Paths.get(ClusterPath.PG_BASE_PATH.path()))) {
      failValidation("walPath must be a subdirectory of " + ClusterPath.PG_BASE_PATH.path()
          + ", not the path itself", field);
      return;
    }
    if (path.startsWith(Paths.get(ClusterPath.PG_BASE_PATH.path()))) {
      return;
    }
    Optional<Map.Entry<StackGresClusterPodsCustomPersistentVolume, CustomVolumeMount>>
        hostingVolume = CustomPersistentVolumeUtil.getCustomPersistentVolumes(pods)
        .stream()
        .flatMap(customPersistentVolume -> CustomPersistentVolumeUtil
            .patroniMountsOf(pods, customPersistentVolume.getName())
            .stream()
            .map(mount -> Map.entry(customPersistentVolume, mount)))
        .filter(volumeAndMount -> path.startsWith(
            Paths.get(volumeAndMount.getValue().getMountPath())))
        .findFirst();
    if (hostingVolume.isEmpty()) {
      failValidation("walPath must be under the path of the volume generated from "
          + fieldPrefix + ".pods.persistentVolume (" + ClusterPath.PG_BASE_PATH.path()
          + ") or under a path where a custom persistent volume is mounted in the patroni"
          + " container", field);
      return;
    }
    if (path.equals(Paths.get(hostingVolume.get().getValue().getMountPath()))) {
      // The mount path itself may contain filesystem or StackGres internal files (like
      // lost+found or the data coherence marker files when mounted without a subPath) while
      // initdb and pg_basebackup require the WAL directory to be empty or to not exist.
      failValidation("walPath must be a subdirectory of the mount path "
          + hostingVolume.get().getValue().getMountPath() + " of the custom persistent volume"
          + " \"" + hostingVolume.get().getKey().getName() + "\", not the mount path itself",
          field);
      return;
    }
    if (!CustomPersistentVolumeUtil.isCoherentData(hostingVolume.get().getKey())) {
      failValidation("The custom persistent volume \""
          + hostingVolume.get().getKey().getName()
          + "\" hosting walPath must set coherentData to true", field);
    }
  }

  private void validateUpdate(
      StackGresClusterPods pods,
      List<StackGresClusterPodsCustomPersistentVolume> customPersistentVolumes,
      StackGresClusterPods oldPods,
      List<String> appliedWalPaths,
      String field) throws ValidationFailed {
    final List<StackGresClusterPodsCustomPersistentVolume> oldCustomPersistentVolumes =
        CustomPersistentVolumeUtil.getCustomPersistentVolumes(oldPods);
    for (var oldCustomPersistentVolume : oldCustomPersistentVolumes) {
      final Optional<StackGresClusterPodsCustomPersistentVolume> customPersistentVolume =
          customPersistentVolumes.stream()
          .filter(newCustomPersistentVolume -> newCustomPersistentVolume.getName()
              .equals(oldCustomPersistentVolume.getName()))
          .findFirst();
      if (CustomPersistentVolumeUtil.isCoherentData(oldCustomPersistentVolume)
          && (customPersistentVolume.isEmpty()
              || !CustomPersistentVolumeUtil.isCoherentData(customPersistentVolume.get()))) {
        final boolean hostsAppliedWalPath = appliedWalPaths.stream()
            .anyMatch(appliedWalPath -> CustomPersistentVolumeUtil
                .patroniMountsOf(oldPods, oldCustomPersistentVolume.getName())
                .stream()
                .anyMatch(mount -> Paths.get(appliedWalPath)
                    .startsWith(Paths.get(mount.getMountPath()))));
        if (hostsAppliedWalPath) {
          failValidation("Custom persistent volume \"" + oldCustomPersistentVolume.getName()
              + "\" hosts the WAL path currently applied to a Pod: change walPath and restart"
              + " all the Pods before removing it or setting coherentData to false", field);
        }
        if (!CustomPersistentVolumeUtil.isAllowCoherentDataRemoval(oldCustomPersistentVolume)) {
          failValidation("Custom persistent volume \"" + oldCustomPersistentVolume.getName()
              + "\" with coherentData set to true can not be removed (nor coherentData set to"
              + " false) unless allowCoherentDataRemoval was previously set to true", field);
        }
      }
    }
    for (var oldCustomPersistentVolume : oldCustomPersistentVolumes) {
      final Optional<StackGresClusterPodsCustomPersistentVolume> customPersistentVolume =
          customPersistentVolumes.stream()
          .filter(newCustomPersistentVolume -> newCustomPersistentVolume.getName()
              .equals(oldCustomPersistentVolume.getName()))
          .findFirst();
      if (customPersistentVolume.isPresent()
          && customPersistentVolume.get().getSize() != null
          && oldCustomPersistentVolume.getSize() != null
          && Quantity.getAmountInBytes(new Quantity(customPersistentVolume.get().getSize()))
              .compareTo(Quantity.getAmountInBytes(
                  new Quantity(oldCustomPersistentVolume.getSize()))) < 0) {
        failValidation("Decrease of persistent volume size is not supported for custom"
            + " persistent volume \"" + oldCustomPersistentVolume.getName() + "\"", field);
      }
    }
  }

}
