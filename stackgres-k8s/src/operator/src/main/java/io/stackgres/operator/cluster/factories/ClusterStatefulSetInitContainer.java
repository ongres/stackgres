/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factories;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.patroni.PatroniConfigMap;

import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ClusterStatefulSetInitContainer {

  private final ClusterStatefulSetEnvironmentVariablesFactory environmentVariablesFactory;

  @Inject
  public ClusterStatefulSetInitContainer(
      ClusterStatefulSetEnvironmentVariablesFactory environmentVariablesFactory) {
    this.environmentVariablesFactory = environmentVariablesFactory;
  }

  public ImmutableList<Container> getInitContainers(StackGresClusterContext config) {
    ImmutableList<EnvVar> patroniSetEnvVariables = environmentVariablesFactory
        .getPatroniEnvironmentVariables(config);
    ImmutableList<EnvVar> backupSetEnvVariables = environmentVariablesFactory
        .getBackupEnvironmentVariables(config);
    ImmutableList<EnvVar> restoreSetEnvVariables = environmentVariablesFactory
        .getRestoreEnvironmentVariables(config);

    Container container = new ContainerBuilder()
        .withName("set-data-permissions")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", getSetDataPermissionCommand())
        .withVolumeMounts(getSetDataPermissionVolumeMounts(config))
        .build();

    ImmutableList.Builder<Container> containerBuilder = ImmutableList.<Container>builder()
        .add(container);

    config.getRestoreConfigSource().ifPresent(restoreConfigSource -> {
      Container restoreWrapperContainer = new ContainerBuilder()
          .withName("wal-g-restore-wrapper")
          .withImage("busybox")
          .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
              .asCharSource(
                  ClusterStatefulSet.class.getResource("/create-wal-g-restore-wrapper.sh"),
                  StandardCharsets.UTF_8)
              .read()).get())
          .withEnvFrom(new EnvFromSourceBuilder()
              .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                  .withName(PatroniConfigMap.restoreName(config)).build())
              .build())
          .withEnv(ImmutableList.<EnvVar>builder()
              .addAll(patroniSetEnvVariables)
              .addAll(restoreSetEnvVariables)
              .build())
          .withVolumeMounts(
              new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.WAL_G_RESTORE_WRAPPER_VOLUME_NAME)
                  .withMountPath("/wal-g-restore-wrapper")
                  .build())
          .build();

      Container restoreEntryPoint = new ContainerBuilder()
          .withName("restore-entripoint")
          .withImage("busybox")
          .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
              .asCharSource(
                  ClusterStatefulSet.class.getResource("/restore-entripoint.sh"),
                  StandardCharsets.UTF_8)
              .read()).get())
          .withEnvFrom(new EnvFromSourceBuilder()
              .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                  .withName(PatroniConfigMap.patroniName(config)).build())
              .build(),
              new EnvFromSourceBuilder()
              .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                  .withName(PatroniConfigMap.restoreName(config)).build())
              .build())
          .withEnv(ImmutableList.<EnvVar>builder()
              .addAll(patroniSetEnvVariables)
              .addAll(restoreSetEnvVariables)
              .build())
          .withVolumeMounts(
              new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
                  .withMountPath("/restore")
                  .build())
          .build();

      containerBuilder.add(restoreWrapperContainer);
      containerBuilder.add(restoreEntryPoint);
    });

    config.getBackupConfig().ifPresent(backupConfig -> {
      Container backup = new ContainerBuilder()
          .withName("wal-g-wrapper")
          .withImage("busybox")
          .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
              .asCharSource(
                  ClusterStatefulSet.class.getResource("/create-wal-g-wrapper.sh"),
                  StandardCharsets.UTF_8)
              .read()).get())
          .withEnvFrom(new EnvFromSourceBuilder()
              .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                  .withName(PatroniConfigMap.backupName(config)).build())
              .build())
          .withEnv(ImmutableList.<EnvVar>builder()
              .addAll(patroniSetEnvVariables)
              .addAll(backupSetEnvVariables)
              .build())
          .withVolumeMounts(
              new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.WAL_G_WRAPPER_VOLUME_NAME)
                  .withMountPath("/wal-g-wrapper")
                  .build())
          .build();
      containerBuilder.add(backup);
    });

    return containerBuilder.build();
  }

  private VolumeMount[] getSetDataPermissionVolumeMounts(StackGresClusterContext config) {
    final String name = config.getCluster().getMetadata().getName();
    return Stream.of(
        Stream.of(new VolumeMountBuilder()
            .withName(name + ClusterStatefulSet.DATA_SUFFIX)
            .withMountPath(ClusterStatefulSet.PG_VOLUME_PATH)
            .build()))
        .flatMap(s -> s)
        .toArray(VolumeMount[]::new);
  }

  private String getSetDataPermissionCommand() {
    return Stream.of(
        Stream.of(
            "chmod -R 700 " + ClusterStatefulSet.PG_VOLUME_PATH,
            "chown -R 999:999 " + ClusterStatefulSet.PG_VOLUME_PATH))
        .flatMap(s -> s)
        .collect(Collectors.joining(" && "));
  }

}
