/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factories;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;

@ApplicationScoped
public class ClusterStatefulSetVolumeMountFactory {

  private static final ImmutableMap<String, Supplier<VolumeMount>> VOLUME_MOUNTS =
      ImmutableMap.<String, Supplier<VolumeMount>>builder()
          .put(ClusterStatefulSet.WAL_G_WRAPPER_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.WAL_G_WRAPPER_VOLUME_NAME)
              .withMountPath("/wal-g-wrapper")
              .build())
          .put(ClusterStatefulSet.WAL_G_RESTORE_WRAPPER_VOLUME_NAME,
              () -> new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.WAL_G_RESTORE_WRAPPER_VOLUME_NAME)
                  .withMountPath("/wal-g-restore-wrapper")
                  .build())
          .put(ClusterStatefulSet.GCS_CREDENTIALS_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.GCS_CREDENTIALS_VOLUME_NAME)
              .withMountPath(ClusterStatefulSet.GCS_CONFIG_PATH)
              .build())
          .put(ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_VOLUME_NAME,
              () -> new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_VOLUME_NAME)
                  .withMountPath(ClusterStatefulSet.GCS_RESTORE_CONFIG_PATH)
                  .build())
          .put(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
              .withMountPath(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME_PATH)
              .build())
          .build();

  private final ClusterStatefulSetVolumeFactory volumeFactory;

  @Inject
  public ClusterStatefulSetVolumeMountFactory(
      ClusterStatefulSetVolumeFactory volumeFactory) {
    this.volumeFactory = volumeFactory;
  }

  public ImmutableList<VolumeMount> getVolumeMounts(StackGresClusterContext config) {

    final String name = config.getCluster().getMetadata().getName();

    ImmutableList.Builder<VolumeMount> volumeMountListBuilder =
        ImmutableList.<VolumeMount>builder().add(
            new VolumeMountBuilder()
                .withName(ClusterStatefulSet.SOCKET_VOLUME_NAME)
                .withMountPath("/run/postgresql")
                .build(),
            new VolumeMountBuilder()
                .withName(name + ClusterStatefulSet.DATA_SUFFIX)
                .withMountPath(ClusterStatefulSet.PG_VOLUME_PATH)
                .build()
        );

    Map<String, Supplier<VolumeMount>> allMounts = getConfigurableVolumeMounts(name);

    withVolumeNames(config, (volumes) -> {

      List<VolumeMount> mounts = volumes.filter(allMounts::containsKey)
          .map(volume -> allMounts.get(volume).get())
          .collect(Collectors.toList());

      volumeMountListBuilder.addAll(mounts);

    });

    return volumeMountListBuilder.build();
  }

  private ImmutableMap<String, Supplier<VolumeMount>> getConfigurableVolumeMounts(String name) {
    return ImmutableMap.<String, Supplier<VolumeMount>>builder()
        .putAll(VOLUME_MOUNTS)
        .put(name + ClusterStatefulSet.BACKUP_SUFFIX, () -> new VolumeMountBuilder()
            .withName(name + ClusterStatefulSet.BACKUP_SUFFIX)
            .withMountPath(ClusterStatefulSet.BACKUP_VOLUME_PATH)
            .build())
        .build();
  }

  private void withVolumeNames(StackGresClusterContext config, Consumer<Stream<String>> f) {
    ImmutableList<Volume> volumeMounts = volumeFactory.getVolumes(config);
    Stream<String> volumeStream = volumeMounts.stream().map(Volume::getName);
    f.accept(volumeStream);
  }
}
