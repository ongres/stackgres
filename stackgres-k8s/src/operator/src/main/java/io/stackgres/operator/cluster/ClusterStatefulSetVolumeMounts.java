/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class ClusterStatefulSetVolumeMounts
    implements SubResourceStreamFactory<VolumeMount, StackGresClusterContext> {

  private static final ImmutableMap<String, Supplier<VolumeMount>> VOLUME_MOUNTS =
      ImmutableMap.<String, Supplier<VolumeMount>>builder()
          .put(ClusterStatefulSet.PATRONI_CONFIG_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.PATRONI_CONFIG_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.PATRONI_ENV_PATH)
              .build())
          .put(ClusterStatefulSet.BACKUP_CONFIG_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.BACKUP_CONFIG_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.BACKUP_ENV_PATH)
              .build())
          .put(ClusterStatefulSet.BACKUP_SECRET_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.BACKUP_SECRET_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.BACKUP_SECRET_PATH)
              .build())
          .put(ClusterStatefulSet.RESTORE_CONFIG_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_CONFIG_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.RESTORE_ENV_PATH)
              .build())
          .put(ClusterStatefulSet.RESTORE_SECRET_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_SECRET_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.RESTORE_SECRET_PATH)
              .build())
          .put(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.RESTORE_ENTRYPOINT_PATH)
              .build())
          .put(ClusterStatefulSet.LOCAL_BIN_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.LOCAL_BIN_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.LOCAL_BIN_PATH)
              .build())
          .build();

  @Override
  public Stream<VolumeMount> create(StackGresClusterContext config) {
    ImmutableList.Builder<VolumeMount> volumeMountListBuilder =
        ImmutableList.<VolumeMount>builder().add(
            new VolumeMountBuilder()
                .withName(ClusterStatefulSet.SOCKET_VOLUME_NAME)
                .withMountPath(ClusterStatefulSet.PG_RUN_PATH)
                .build(),
            new VolumeMountBuilder()
                .withName(ClusterStatefulSet.dataName(config))
                .withMountPath(ClusterStatefulSet.PG_BASE_PATH)
                .build()
        );

    Map<String, Supplier<VolumeMount>> allMounts = getConfigurableVolumeMounts(config);

    withVolumeNames(config, (volumes) -> {

      List<VolumeMount> mounts = volumes.filter(allMounts::containsKey)
          .map(volume -> allMounts.get(volume).get())
          .collect(Collectors.toList());

      volumeMountListBuilder.addAll(mounts);

    });

    return volumeMountListBuilder.build().stream();
  }

  private ImmutableMap<String, Supplier<VolumeMount>> getConfigurableVolumeMounts(
      StackGresClusterContext config) {
    return ImmutableMap.<String, Supplier<VolumeMount>>builder()
        .putAll(VOLUME_MOUNTS)
        .put(ClusterStatefulSet.backupName(config), () -> new VolumeMountBuilder()
            .withName(ClusterStatefulSet.backupName(config))
            .withMountPath(ClusterStatefulSet.BACKUP_PATH)
            .build())
        .build();
  }

  private void withVolumeNames(StackGresClusterContext config, Consumer<Stream<String>> f) {
    f.accept(new ClusterStatefulSetVolumes()
        .create(config)
        .map(Volume::getName));
  }

}
