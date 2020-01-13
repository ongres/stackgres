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
import io.stackgres.operatorframework.factories.EnvironmentVariablesFactory;
import io.stackgres.operatorframework.factories.InitContainerFactory;

import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ClusterStatefulSetInitContainer
    implements InitContainerFactory<StackGresClusterContext> {

  private EnvironmentVariablesFactory<StackGresClusterContext> environmentVariablesFactory;

  @Inject
  public ClusterStatefulSetInitContainer(
      EnvironmentVariablesFactory<StackGresClusterContext> environmentVariablesFactory) {
    this.environmentVariablesFactory = environmentVariablesFactory;
  }

  @Override
  public ImmutableList<Container> getInitContainers(StackGresClusterContext config) {
    final String name = config.getCluster().getMetadata().getName();

    ImmutableList<EnvVar> statefulSetEnvVariables = environmentVariablesFactory
        .getEnvironmentVariables(config);

    Container container = new ContainerBuilder()
        .withName("data-permissions")
        .withImage("busybox")
        .withCommand("/bin/sh", "-ecx", getPermissionCommand(config))
        .withVolumeMounts(getPermissionVolumeMounts(config))
        .build();

    ImmutableList.Builder<Container> containerBuilder = ImmutableList.<Container>builder()
        .add(container);

    config.getRestoreConfig().ifPresent(restoreConfig -> {
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
                  .withName(name).build())
              .build())
          .withEnv(statefulSetEnvVariables)
          .withVolumeMounts(
              new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.WAL_G_RESTORE_WRAPPER_VOLUME_NAME)
                  .withMountPath("/wal-g-restore-wrapper")
                  .build())
          .build();

      Container restoreEntripoint = new ContainerBuilder()
          .withName("restore-entripoint")
          .withImage("busybox")
          .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
              .asCharSource(
                  ClusterStatefulSet.class.getResource("/restore-entripoint.sh"),
                  StandardCharsets.UTF_8)
              .read()).get())
          .withEnvFrom(new EnvFromSourceBuilder()
              .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                  .withName(name).build())
              .build())
          .withEnv(statefulSetEnvVariables)
          .withVolumeMounts(
              new VolumeMountBuilder()
                  .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
                  .withMountPath("/restore")
                  .build())
          .build();

      containerBuilder.add(restoreWrapperContainer);
      containerBuilder.add(restoreEntripoint);
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
                  .withName(name).build())
              .build())
          .withEnv(statefulSetEnvVariables)
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

  private VolumeMount[] getPermissionVolumeMounts(StackGresClusterContext config) {
    final String name = config.getCluster().getMetadata().getName();
    return Stream.of(
        Stream.of(new VolumeMountBuilder()
            .withName(name + ClusterStatefulSet.DATA_SUFFIX)
            .withMountPath(ClusterStatefulSet.PG_VOLUME_PATH)
            .build()))
        .flatMap(s -> s)
        .toArray(VolumeMount[]::new);
  }

  private String getPermissionCommand(StackGresClusterContext config) {
    return Stream.of(
        Stream.of(
            "chmod -R 700 " + ClusterStatefulSet.PG_VOLUME_PATH,
            "chown -R 999:999 " + ClusterStatefulSet.PG_VOLUME_PATH))
        .flatMap(s -> s)
        .collect(Collectors.joining(" && "));
  }

}
