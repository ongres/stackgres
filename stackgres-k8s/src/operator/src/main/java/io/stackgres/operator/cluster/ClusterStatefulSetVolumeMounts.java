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

  public static final String PATRONI_ENV = "/etc/env/patroni";
  public static final String BACKUP_ENV = "/etc/env/backup";
  public static final String BACKUP_SECRET = "/etc/secret/backup";
  public static final String RESTORE_ENV = "/etc/env/restore";
  public static final String RESTORE_SECRET = "/etc/secret/restore";

  private static final ImmutableMap<String, Supplier<VolumeMount>> VOLUME_MOUNTS =
      ImmutableMap.<String, Supplier<VolumeMount>>builder()
          .put(ClusterStatefulSet.PATRONI_CONFIG_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.PATRONI_CONFIG_VOLUME_NAME)
              .withMountPath(PATRONI_ENV)
              .build())
          .put(ClusterStatefulSet.BACKUP_CONFIG_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.BACKUP_CONFIG_VOLUME_NAME)
              .withMountPath(BACKUP_ENV)
              .build())
          .put(ClusterStatefulSet.BACKUP_SECRET_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.BACKUP_SECRET_VOLUME_NAME)
              .withMountPath(BACKUP_SECRET)
              .build())
          .put(ClusterStatefulSet.RESTORE_CONFIG_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_CONFIG_VOLUME_NAME)
              .withMountPath(RESTORE_ENV)
              .build())
          .put(ClusterStatefulSet.RESTORE_SECRET_VOLUME_NAME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_SECRET_VOLUME_NAME)
              .withMountPath(RESTORE_SECRET)
              .build())
          .put(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME, () -> new VolumeMountBuilder()
              .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
              .withMountPath(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME_PATH)
              .build())
          .build();

  @Override
  public Stream<VolumeMount> create(StackGresClusterContext config) {

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

    return volumeMountListBuilder.build().stream();
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
    f.accept(new ClusterStatefulSetVolumes()
        .create(config)
        .map(Volume::getName));
  }
}
